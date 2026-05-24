// @vitest-environment jsdom
import { describe, it, expect } from 'vitest';
import { deriveKeyFromPassword, encryptPrivateKey, decryptPrivateKey, generateNonce } from '../../crypto/keyManagement';

describe('KeyManagement', () => {
  it('deriveKeyFromPassword returns CryptoKey', async () => {
    const salt = crypto.getRandomValues(new Uint8Array(16));
    const key = await deriveKeyFromPassword('password123', salt);
    expect(key).toBeDefined();
    expect(key.type).toBe('secret');
  });

  it('encryptPrivateKey + decryptPrivateKey round-trip', async () => {
    const data = new TextEncoder().encode('private key data here');
    const password = 'mySecurePassword';
    const { encrypted, iv } = await encryptPrivateKey(data, password);
    const decrypted = await decryptPrivateKey(encrypted, iv, password);
    expect(new TextDecoder().decode(decrypted)).toBe(new TextDecoder().decode(data));
  });

  it('decryptPrivateKey with wrong password throws', async () => {
    const data = new TextEncoder().encode('private key data');
    const { encrypted, iv } = await encryptPrivateKey(data, 'correctPassword');
    await expect(decryptPrivateKey(encrypted, iv, 'wrongPassword')).rejects.toThrow();
  });

  it('generateNonce returns correct length', () => {
    const nonce16 = generateNonce(16);
    expect(nonce16).toBeInstanceOf(Uint8Array);
    expect(nonce16.length).toBe(16);
    const nonce32 = generateNonce(32);
    expect(nonce32.length).toBe(32);
  });
});
