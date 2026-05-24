import type { User } from '../types';
import { apiFetch } from './client';

export async function getProfile(): Promise<User> {
  return apiFetch('/users/me');
}

export async function searchUsers(query: string): Promise<User[]> {
  return apiFetch(`/users/search?q=${encodeURIComponent(query)}`);
}

export async function getPublicKey(userId: number): Promise<string> {
  const response = await apiFetch<{ publicKeyPem: string }>(`/users/${userId}/public-key`);
  return response.publicKeyPem;
}
