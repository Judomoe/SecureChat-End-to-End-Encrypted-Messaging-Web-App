// @vitest-environment jsdom
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { apiFetch, setAuthToken, setOnUnauthorized } from '../../api/client';

describe('apiFetch', () => {
  beforeEach(() => {
    setAuthToken(null);
    setOnUnauthorized(() => {});
    vi.restoreAllMocks();
  });

  it('apiFetch attaches Authorization header when token is set', async () => {
    setAuthToken('test-token');
    const mockFetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve({ data: 'test' }),
    });
    vi.stubGlobal('fetch', mockFetch);

    await apiFetch('/test');

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/v1/test',
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: 'Bearer test-token',
        }),
      }),
    );
  });

  it('apiFetch calls onUnauthorized callback on 401', async () => {
    const callback = vi.fn();
    setOnUnauthorized(callback);
    const mockFetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
    });
    vi.stubGlobal('fetch', mockFetch);

    await expect(apiFetch('/test')).rejects.toThrow('Unauthorized');
    expect(callback).toHaveBeenCalled();
  });

  it('apiFetch handles network errors', async () => {
    const mockFetch = vi.fn().mockRejectedValue(new TypeError('Failed to fetch'));
    vi.stubGlobal('fetch', mockFetch);

    await expect(apiFetch('/test')).rejects.toThrow('Failed to fetch');
  });
});
