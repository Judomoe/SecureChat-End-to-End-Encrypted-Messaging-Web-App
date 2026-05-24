import { useState, useCallback } from 'react';
import type { Conversation } from '../types';
import * as conversationsApi from '../api/conversations';
import * as keysApi from '../api/keys';
import * as rsa from '../crypto/rsa';
import * as aes from '../crypto/aes';
import { arrayBufferToBase64 } from '../crypto/aes';
import * as usersApi from '../api/users';
import { useAuth } from '../context/AuthContext';

export function useConversations() {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [activeConversation, setActiveConversation] = useState<Conversation | null>(null);
  const [loading, setLoading] = useState(false);
  const { getConversationKey, setConversationKey, signingKey, privateKey } = useAuth();

  const loadConversations = useCallback(async () => {
    setLoading(true);
    try {
      const convs = await conversationsApi.listConversations();
      setConversations(convs);
    } catch {
      // ignore
    } finally {
      setLoading(false);
    }
  }, []);

  const createConversation = useCallback(
    async (recipientId: number) => {
      const existing = conversations.find((c) =>
        c.members.some((m) => m.userId === recipientId)
      );
      if (existing) {
        return existing;
      }

      setLoading(true);
      try {
        const conversation = await conversationsApi.createConversation(recipientId);
        const aesKey = await aes.generateAesKey();
        const rawKey = await aes.exportRawKey(aesKey);

        const publicKeyPem = await usersApi.getPublicKey(recipientId);
        const recipientPublicKey = await rsa.importPublicKeyPem(publicKeyPem);

        const encryptedKeyBytes = await rsa.rsaOaepEncrypt(recipientPublicKey, rawKey);
        const encryptedAesKey = arrayBufferToBase64(encryptedKeyBytes.buffer as ArrayBuffer);

        const signatureBytes = await rsa.rsaPssSign(signingKey!, encryptedKeyBytes);
        const signature = arrayBufferToBase64(signatureBytes.buffer as ArrayBuffer);

        await keysApi.exchangeKey(conversation.id, recipientId, encryptedAesKey, signature);

        setConversationKey(conversation.id, aesKey);
        await loadConversations();
        return conversation;
      } finally {
        setLoading(false);
      }
    },
    [signingKey, setConversationKey, loadConversations, conversations],
  );

  const selectConversation = useCallback(
    async (conversation: Conversation) => {
      setActiveConversation(conversation);

      if (getConversationKey(conversation.id)) {
        return;
      }

      try {
        const keyMaterial = await keysApi.fetchPendingKey(conversation.id);

        const senderId = keyMaterial.senderId;

        const senderPubKeyPem = await usersApi.getPublicKey(senderId);
        const senderVerifyKey = await rsa.importPublicKeyPemForVerify(senderPubKeyPem);

        const encKeyBytes = aes.base64ToArrayBuffer(keyMaterial.encryptedAesKey);
        const sigBytes = aes.base64ToArrayBuffer(keyMaterial.signature);

        const valid = await rsa.rsaPssVerify(senderVerifyKey, encKeyBytes, sigBytes);
        if (!valid) {
          throw new Error('Invalid key signature');
        }

        const decryptedKeyBytes = await rsa.rsaOaepDecrypt(privateKey!, encKeyBytes);

        const importedKey = await crypto.subtle.importKey(
          'raw',
          decryptedKeyBytes as Uint8Array<ArrayBuffer>,
          { name: 'AES-GCM', length: 256 },
          true,
          ['encrypt', 'decrypt'],
        );

        setConversationKey(conversation.id, importedKey);
        await keysApi.confirmKey(conversation.id);
      } catch {
        // ignore key fetch errors
      }
    },
    [getConversationKey, setConversationKey, privateKey],
  );

  return {
    conversations,
    activeConversation,
    loading,
    loadConversations,
    createConversation,
    selectConversation,
  };
}
