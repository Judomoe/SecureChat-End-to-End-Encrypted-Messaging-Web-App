// @vitest-environment jsdom
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { useConversations } from '../../hooks/useConversations';

vi.mock('../../context/AuthContext', () => ({
  useAuth: () => ({
    getConversationKey: vi.fn().mockReturnValue(undefined),
    setConversationKey: vi.fn(),
    signingKey: null,
    privateKey: null,
  }),
}));

vi.mock('../../api/conversations', () => ({
  listConversations: vi.fn(),
  createConversation: vi.fn(),
  getConversation: vi.fn(),
}));

vi.mock('../../api/keys', () => ({
  exchangeKey: vi.fn().mockResolvedValue({}),
  fetchPendingKey: vi.fn(),
  confirmKey: vi.fn().mockResolvedValue(undefined),
}));

vi.mock('../../crypto/rsa', () => ({
  importPublicKeyPem: vi.fn().mockResolvedValue({}),
  rsaOaepEncrypt: vi.fn().mockResolvedValue(new Uint8Array([1, 2, 3])),
  rsaPssSign: vi.fn().mockResolvedValue(new Uint8Array([4, 5, 6])),
}));

vi.mock('../../crypto/aes', () => ({
  generateAesKey: vi.fn().mockResolvedValue({}),
  exportRawKey: vi.fn().mockResolvedValue(new Uint8Array([1, 2, 3])),
  arrayBufferToBase64: vi.fn().mockReturnValue('base64value'),
}));

vi.mock('../../api/users', () => ({
  getPublicKey: vi.fn().mockResolvedValue('public-key-pem'),
}));

import { listConversations, createConversation } from '../../api/conversations';

describe('useConversations', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('listConversations fetches from API', async () => {
    const mockConvs = [
      { id: 1, members: [{ id: 1, userId: 1, username: 'user1', joinedAt: '' }], createdAt: '2024-01-01T00:00:00Z' },
    ];
    vi.mocked(listConversations).mockResolvedValue(mockConvs as never);

    const { result } = renderHook(() => useConversations());

    await result.current.loadConversations();

    await waitFor(() => {
      expect(listConversations).toHaveBeenCalled();
      expect(result.current.conversations).toEqual(mockConvs);
    });
  });

  it('createConversation calls API', async () => {
    const mockConv = { id: 1, members: [], createdAt: '2024-01-01T00:00:00Z' };
    vi.mocked(createConversation).mockResolvedValue(mockConv as never);
    vi.mocked(listConversations).mockResolvedValue([]);

    const { result } = renderHook(() => useConversations());

    const convResult = await result.current.createConversation(2);

    expect(createConversation).toHaveBeenCalledWith(2);
    expect(convResult).toEqual(mockConv);
  });
});
