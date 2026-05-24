// @vitest-environment jsdom
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { register, login } from '../../api/auth';

vi.mock('../../api/client', () => ({
  apiFetch: vi.fn(),
}));

import { apiFetch } from '../../api/client';
const mockedApiFetch = vi.mocked(apiFetch);

describe('auth API', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('register sends correct payload', async () => {
    const mockResponse = {
      id: 1,
      username: 'testuser',
      email: 'test@example.com',
      displayName: 'Test',
      createdAt: '2024-01-01T00:00:00Z',
    };
    mockedApiFetch.mockResolvedValue(mockResponse);

    const result = await register({
      username: 'testuser',
      email: 'test@example.com',
      password: 'password123',
      displayName: 'Test',
      publicKeyPem: '-----BEGIN PUBLIC KEY-----\ntest\n-----END PUBLIC KEY-----',
      encryptedPrivateKey: 'encrypted',
      privateKeyIv: 'iv',
    });

    expect(mockedApiFetch).toHaveBeenCalledWith('/auth/register', {
      method: 'POST',
      body: expect.any(String),
    });
    expect(result).toEqual(mockResponse);
  });

  it('login returns token and user data', async () => {
    const mockResponse = {
      token: 'jwt-token',
      user: { id: 1, username: 'testuser', email: 'test@example.com', displayName: 'Test', createdAt: '2024-01-01T00:00:00Z' },
      encryptedPrivateKey: 'encrypted',
      privateKeyIv: 'iv',
    };
    mockedApiFetch.mockResolvedValue(mockResponse);

    const result = await login('test@example.com', 'password123');

    expect(mockedApiFetch).toHaveBeenCalledWith('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email: 'test@example.com', password: 'password123' }),
    });
    expect(result.token).toBe('jwt-token');
    expect(result.user.username).toBe('testuser');
  });
});
