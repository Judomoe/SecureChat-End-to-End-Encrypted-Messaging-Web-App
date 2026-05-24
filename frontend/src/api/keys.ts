import type { KeyMaterial } from '../types';
import { apiFetch } from './client';

export async function exchangeKey(
  conversationId: number,
  recipientId: number,
  encryptedAesKey: string,
  signature: string,
): Promise<KeyMaterial> {
  return apiFetch(`/conversations/${conversationId}/keys/exchange`, {
    method: 'POST',
    body: JSON.stringify({ recipientId, encryptedAesKey, signature }),
  });
}

export async function fetchPendingKey(conversationId: number): Promise<KeyMaterial> {
  return apiFetch(`/conversations/${conversationId}/keys/pending`);
}

export async function confirmKey(conversationId: number): Promise<void> {
  return apiFetch(`/conversations/${conversationId}/keys/confirm`, {
    method: 'POST',
  });
}
