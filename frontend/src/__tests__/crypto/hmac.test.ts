// @vitest-environment jsdom
import { describe, it, expect } from 'vitest';
import { importHmacKey, hmacSha256, hmacVerify, exportRawKey } from '../../crypto/hmac';

describe('HMAC', () => {
  it('importHmacKey creates HMAC CryptoKey', async () => {
    const rawKey = crypto.getRandomValues(new Uint8Array(32));
    const key = await importHmacKey(rawKey);
    expect(key).toBeDefined();
    expect(key.type).toBe('secret');
    expect(key.algorithm.name).toBe('HMAC');
  });

  it('hmacSha256 returns base64 string', async () => {
    const rawKey = crypto.getRandomValues(new Uint8Array(32));
    const key = await importHmacKey(rawKey);
    const result = await hmacSha256(key, 'test message');
    expect(typeof result).toBe('string');
    expect(() => atob(result)).not.toThrow();
  });

  it('hmacVerify with correct HMAC returns true', async () => {
    const rawKey = crypto.getRandomValues(new Uint8Array(32));
    const key = await importHmacKey(rawKey);
    const message = 'test message';
    const hmacValue = await hmacSha256(key, message);
    const isValid = await hmacVerify(key, message, hmacValue);
    expect(isValid).toBe(true);
  });

  it('hmacVerify with wrong HMAC returns false', async () => {
    const rawKey = crypto.getRandomValues(new Uint8Array(32));
    const key = await importHmacKey(rawKey);
    const isValid = await hmacVerify(key, 'test message', 'aW52YWxpZGhtYWM=');
    expect(isValid).toBe(false);
  });

  it('exportRawKey returns raw bytes', async () => {
    const rawKey = crypto.getRandomValues(new Uint8Array(32));
    const key = await importHmacKey(rawKey);
    const exported = await exportRawKey(key);
    expect(exported).toBeInstanceOf(Uint8Array);
    expect(exported.length).toBe(32);
  });
});
