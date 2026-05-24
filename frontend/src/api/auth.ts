import type { RegisterRequest, LoginResponse } from '../types';
import { apiFetch } from './client';

export async function register(
  req: RegisterRequest,
): Promise<{ id: number; username: string; email: string; displayName: string; createdAt: string }> {
  return apiFetch('/auth/register', {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

export async function login(email: string, password: string): Promise<LoginResponse> {
  return apiFetch('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
}
