import type { EncryptedMessage } from '../types';
import { apiFetch } from './client';

export async function sendMessage(
  conversationId: number,
  ciphertext: string,
  iv: string,
  hmac: string,
  signature: string,
  timestamp: number,
): Promise<{ id: number; createdAt: string }> {
  return apiFetch(`/conversations/${conversationId}/messages`, {
    method: 'POST',
    body: JSON.stringify({ ciphertext, iv, hmac, signature, timestamp }),
  });
}

export async function getMessages(
  conversationId: number,
  limit: number = 50,
  before?: number,
): Promise<EncryptedMessage[]> {
  let path = `/conversations/${conversationId}/messages?limit=${limit}`;
  if (before !== undefined) {
    path += `&before=${before}`;
  }
  return apiFetch(path);
}
