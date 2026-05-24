// @vitest-environment jsdom
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { useMessages } from '../../hooks/useMessages';

const mocks = vi.hoisted(() => ({
  getConversationKey: vi.fn().mockReturnValue({} as CryptoKey),
  signingKey: {} as CryptoKey,
  user: { id: 1, username: 'testuser', email: 't@t.com', displayName: 'Test', createdAt: '' },
  sendMessageApi: vi.fn().mockResolvedValue({ id: 1, createdAt: '' }),
  getMessagesApi: vi.fn().mockResolvedValue([]),
  aesGcmEncrypt: vi.fn().mockResolvedValue({ ciphertext: 'ct', iv: 'iv' }),
  exportRawKey: vi.fn().mockResolvedValue(new Uint8Array(32)),
  arrayBufferToBase64: vi.fn().mockReturnValue('base64'),
  base64ToArrayBuffer: vi.fn().mockReturnValue(new Uint8Array([1, 2, 3])),
  importHmacKey: vi.fn().mockResolvedValue({} as CryptoKey),
  hmacSha256: vi.fn().mockResolvedValue('hmachash'),
  rsaPssSign: vi.fn().mockResolvedValue(new Uint8Array([1, 2, 3])),
  getConversationApi: vi.fn().mockResolvedValue({ members: [] }),
}));

vi.mock('../../context/AuthContext', () => ({
  useAuth: () => ({
    getConversationKey: mocks.getConversationKey,
    signingKey: mocks.signingKey,
    user: mocks.user,
  }),
}));

vi.mock('../../api/messages', () => ({
  sendMessage: mocks.sendMessageApi,
  getMessages: mocks.getMessagesApi,
}));

vi.mock('../../api/conversations', () => ({
  getConversation: mocks.getConversationApi,
}));

vi.mock('../../crypto/aes', () => ({
  aesGcmEncrypt: mocks.aesGcmEncrypt,
  exportRawKey: mocks.exportRawKey,
  arrayBufferToBase64: mocks.arrayBufferToBase64,
  base64ToArrayBuffer: mocks.base64ToArrayBuffer,
}));

vi.mock('../../crypto/hmac', () => ({
  importHmacKey: mocks.importHmacKey,
  hmacSha256: mocks.hmacSha256,
}));

vi.mock('../../crypto/rsa', () => ({
  rsaPssSign: mocks.rsaPssSign,
}));

describe('useMessages', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('sendMessage encrypts and sends', async () => {
    const { result } = renderHook(() => useMessages(1));

    await waitFor(() => expect(result.current.loading).toBe(false));

    await act(async () => {
      await result.current.sendMessage('Hello World');
    });

    expect(mocks.aesGcmEncrypt).toHaveBeenCalledWith(expect.anything(), 'Hello World');
    expect(mocks.sendMessageApi).toHaveBeenCalledWith(
      1,
      'ct',
      'iv',
      'hmachash',
      'base64',
      expect.any(Number),
    );
  });

  it('fetchMessages fetches all messages', async () => {
    renderHook(() => useMessages(1));

    await waitFor(() => {
      expect(mocks.getMessagesApi).toHaveBeenCalledWith(1, 1000);
    });
  });
});
