import type { Conversation } from '../types';
import { apiFetch } from './client';

export async function createConversation(recipientId: number): Promise<Conversation> {
  return apiFetch('/conversations', {
    method: 'POST',
    body: JSON.stringify({ recipientId }),
  });
}

export async function listConversations(): Promise<Conversation[]> {
  return apiFetch('/conversations');
}

export async function getConversation(id: number): Promise<Conversation> {
  return apiFetch(`/conversations/${id}`);
}
