// @vitest-environment jsdom
import { describe, it, expect } from 'vitest';
import { generateAesKey, aesGcmEncrypt, aesGcmDecrypt } from '../../crypto/aes';

describe('AES', () => {
  it('generateAesKey returns a CryptoKey', async () => {
    const key = await generateAesKey();
    expect(key).toBeDefined();
    expect(key.type).toBe('secret');
  });

  it('aesGcmEncrypt + aesGcmDecrypt round-trip', async () => {
    const key = await generateAesKey();
    const plaintext = 'Hello, AES-GCM!';
    const { ciphertext, iv } = await aesGcmEncrypt(key, plaintext);
    const decrypted = await aesGcmDecrypt(key, ciphertext, iv);
    expect(decrypted).toBe(plaintext);
  });

  it('aesGcmDecrypt with wrong key throws', async () => {
    const key1 = await generateAesKey();
    const key2 = await generateAesKey();
    const { ciphertext, iv } = await aesGcmEncrypt(key1, 'secret');
    await expect(aesGcmDecrypt(key2, ciphertext, iv)).rejects.toThrow();
  });

  it('different IVs produce different ciphertext', async () => {
    const key = await generateAesKey();
    const { ciphertext: ct1 } = await aesGcmEncrypt(key, 'same text');
    const { ciphertext: ct2 } = await aesGcmEncrypt(key, 'same text');
    expect(ct1).not.toBe(ct2);
  });
});
