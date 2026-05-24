import { useState, useEffect, useCallback, useRef } from 'react';
import type { DecryptedMessage, EncryptedMessage, ConversationMember } from '../types';
import * as messagesApi from '../api/messages';
import * as conversationsApi from '../api/conversations';
import * as aes from '../crypto/aes';
import * as hmac from '../crypto/hmac';
import * as rsa from '../crypto/rsa';
import { useAuth } from '../context/AuthContext';

const PAGE_SIZE = 1000;

export function useMessages(conversationId: number | null) {
  const [messages, setMessages] = useState<DecryptedMessage[]>([]);
  const [loading, setLoading] = useState(false);
  const [partnerName, setPartnerName] = useState<string>('');
  const [sendError, setSendError] = useState<string | null>(null);
  const pollingRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const knownIdsRef = useRef<Set<number>>(new Set());
  const { getConversationKey, signingKey, user } = useAuth();

  const decryptMessage = useCallback(
    async (msg: EncryptedMessage, aesKey: CryptoKey): Promise<DecryptedMessage | null> => {
      try {
        const rawKey = await aes.exportRawKey(aesKey);
        const hmacKey = await hmac.importHmacKey(rawKey);

        const hmacMessage = msg.ciphertext + msg.iv + String(msg.timestamp);
        const isValid = await hmac.hmacVerify(hmacKey, hmacMessage, msg.hmac);
        if (!isValid) {
          return null;
        }

        const plaintext = await aes.aesGcmDecrypt(aesKey, msg.ciphertext, msg.iv);

        return {
          ...msg,
          plaintext,
          senderUsername: '',
        };
      } catch {
        return null;
      }
    },
    [],
  );

  const fetchMessages = useCallback(async () => {
    if (!conversationId) return;
    const aesKey = getConversationKey(conversationId);
    if (!aesKey) return;

    try {
      const encryptedMessages = await messagesApi.getMessages(conversationId, PAGE_SIZE);
      const newMessages: DecryptedMessage[] = [];

      for (const msg of encryptedMessages) {
        if (knownIdsRef.current.has(msg.id)) continue;
        knownIdsRef.current.add(msg.id);

        const decrypted = await decryptMessage(msg, aesKey);
        if (decrypted) {
          newMessages.push(decrypted);
        }
      }

      if (newMessages.length > 0) {
        setMessages((prev) => {
          const merged = [...prev, ...newMessages];
          merged.sort((a, b) => a.timestamp - b.timestamp);
          return merged;
        });
      }
    } catch {
      // ignore
    }
  }, [conversationId, getConversationKey, decryptMessage]);

  useEffect(() => {
    if (!conversationId) {
      setMessages([]);
      setPartnerName('');
      setSendError(null);
      knownIdsRef.current = new Set();
      return;
    }

    setLoading(true);
    setMessages([]);
    setSendError(null);
    knownIdsRef.current = new Set();

    conversationsApi.getConversation(conversationId).then((conv) => {
      const other = conv.members.find((m: ConversationMember) => m.userId !== user?.id);
      if (other) {
        setPartnerName(other.username);
      }
    }).catch(() => {});

    fetchMessages().finally(() => setLoading(false));

    pollingRef.current = setInterval(fetchMessages, 3000);

    return () => {
      if (pollingRef.current) {
        clearInterval(pollingRef.current);
        pollingRef.current = null;
      }
    };
  }, [conversationId, fetchMessages, user]);

  const sendMessage = useCallback(
    async (text: string) => {
      if (!conversationId || !signingKey) {
        setSendError('Not authenticated');
        return;
      }
      const aesKey = getConversationKey(conversationId);
      if (!aesKey) {
        setSendError('Conversation key not available — try selecting the conversation from the Chats tab first');
        return;
      }

      setSendError(null);
      try {
        const { ciphertext, iv } = await aes.aesGcmEncrypt(aesKey, text);
        const timestamp = Date.now();

        const rawKey = await aes.exportRawKey(aesKey);
        const hmacKey = await hmac.importHmacKey(rawKey);
        const hmacValue = await hmac.hmacSha256(hmacKey, ciphertext + iv + String(timestamp));

        const hmacRaw = aes.base64ToArrayBuffer(hmacValue);
        const signatureBytes = await rsa.rsaPssSign(signingKey, hmacRaw);
        const signature = aes.arrayBufferToBase64(signatureBytes.buffer as ArrayBuffer);

        const result = await messagesApi.sendMessage(
          conversationId,
          ciphertext,
          iv,
          hmacValue,
          signature,
          timestamp,
        );

        const decrypted: DecryptedMessage = {
          id: result.id,
          senderId: user!.id,
          ciphertext,
          iv,
          hmac: hmacValue,
          signature,
          timestamp,
          plaintext: text,
          senderUsername: user!.username,
        };

        knownIdsRef.current.add(decrypted.id);
        setMessages((prev) => [...prev, decrypted]);
      } catch (err: unknown) {
        const message = err instanceof Error ? err.message : 'Failed to send message';
        setSendError(message);
      }
    },
    [conversationId, signingKey, getConversationKey, user],
  );

  return { messages, sendMessage, loading, partnerName, sendError };
}
