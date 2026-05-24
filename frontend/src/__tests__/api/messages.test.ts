// @vitest-environment jsdom
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { sendMessage, getMessages } from '../../api/messages';

vi.mock('../../api/client', () => ({
  apiFetch: vi.fn(),
}));

import { apiFetch } from '../../api/client';
const mockedApiFetch = vi.mocked(apiFetch);

describe('messages API', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('sendMessage sends correct payload', async () => {
    const mockResponse = { id: 1, createdAt: '2024-01-01T00:00:00Z' };
    mockedApiFetch.mockResolvedValue(mockResponse);

    const result = await sendMessage(1, 'ciphertext', 'iv', 'hmac', 'signature', 1700000000);

    expect(mockedApiFetch).toHaveBeenCalledWith('/conversations/1/messages', {
      method: 'POST',
      body: JSON.stringify({
        ciphertext: 'ciphertext',
        iv: 'iv',
        hmac: 'hmac',
        signature: 'signature',
        timestamp: 1700000000,
      }),
    });
    expect(result).toEqual(mockResponse);
  });

  it('getMessages returns message array', async () => {
    const mockMessages = [
      { id: 1, senderId: 1, ciphertext: 'ct', iv: 'iv', hmac: 'h', signature: 's', timestamp: 1700000000 },
    ];
    mockedApiFetch.mockResolvedValue(mockMessages);

    const result = await getMessages(1);

    expect(mockedApiFetch).toHaveBeenCalledWith('/conversations/1/messages?limit=50');
    expect(result).toEqual(mockMessages);
  });

  it('getMessages passes pagination params', async () => {
    mockedApiFetch.mockResolvedValue([]);

    await getMessages(2, 25, 1700000000);

    expect(mockedApiFetch).toHaveBeenCalledWith('/conversations/2/messages?limit=25&before=1700000000');
  });
});
