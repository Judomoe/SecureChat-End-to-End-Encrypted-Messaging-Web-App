// @vitest-environment jsdom
import { describe, it, expect } from 'vitest';
import {
  generateRsaKeyPair,
  exportPublicKeyPem,
  rsaOaepEncrypt,
  rsaOaepDecrypt,
  rsaPssSign,
  rsaPssVerify,
  importPublicKeyPem,
  importPublicKeyPemForVerify,
} from '../../crypto/rsa';

describe('RSA', () => {
  it('generateRsaKeyPair returns object with encryptionKeyPair and signingPrivateKey/signingPublicKey', async () => {
    const result = await generateRsaKeyPair();
    expect(result.encryptionKeyPair).toBeDefined();
    expect(result.encryptionKeyPair.publicKey).toBeDefined();
    expect(result.encryptionKeyPair.privateKey).toBeDefined();
    expect(result.signingPrivateKey).toBeDefined();
    expect(result.signingPublicKey).toBeDefined();
  });

  it('exportPublicKeyPem returns PEM-formatted string', async () => {
    const { encryptionKeyPair } = await generateRsaKeyPair();
    const pem = await exportPublicKeyPem(encryptionKeyPair.publicKey);
    expect(pem).toContain('-----BEGIN PUBLIC KEY-----');
    expect(pem).toContain('-----END PUBLIC KEY-----');
  });

  it('rsaOaepEncrypt + rsaOaepDecrypt round-trip', async () => {
    const { encryptionKeyPair } = await generateRsaKeyPair();
    const data = new TextEncoder().encode('Hello, RSA!');
    const encrypted = await rsaOaepEncrypt(encryptionKeyPair.publicKey, data);
    const decrypted = await rsaOaepDecrypt(encryptionKeyPair.privateKey, encrypted);
    expect(new TextDecoder().decode(decrypted)).toBe('Hello, RSA!');
  });

  it('rsaOaepDecrypt with wrong key fails', async () => {
    const pair1 = await generateRsaKeyPair();
    const pair2 = await generateRsaKeyPair();
    const data = new TextEncoder().encode('Secret');
    const encrypted = await rsaOaepEncrypt(pair1.encryptionKeyPair.publicKey, data);
    await expect(rsaOaepDecrypt(pair2.encryptionKeyPair.privateKey, encrypted)).rejects.toThrow();
  });

  it('rsaPssSign + rsaPssVerify round-trip', async () => {
    const { signingPrivateKey, signingPublicKey } = await generateRsaKeyPair();
    const data = new TextEncoder().encode('Sign this');
    const signature = await rsaPssSign(signingPrivateKey, data);
    const valid = await rsaPssVerify(signingPublicKey, data, signature);
    expect(valid).toBe(true);
  });

  it('rsaPssVerify with wrong data returns false', async () => {
    const { signingPrivateKey, signingPublicKey } = await generateRsaKeyPair();
    const data = new TextEncoder().encode('Sign this');
    const signature = await rsaPssSign(signingPrivateKey, data);
    const wrongData = new TextEncoder().encode('Wrong data');
    const valid = await rsaPssVerify(signingPublicKey, wrongData, signature);
    expect(valid).toBe(false);
  });

  it('importPublicKeyPem imports a key for encryption', async () => {
    const { encryptionKeyPair } = await generateRsaKeyPair();
    const pem = await exportPublicKeyPem(encryptionKeyPair.publicKey);
    const imported = await importPublicKeyPem(pem);
    const data = new TextEncoder().encode('Test import');
    const encrypted = await rsaOaepEncrypt(imported, data);
    const decrypted = await rsaOaepDecrypt(encryptionKeyPair.privateKey, encrypted);
    expect(new TextDecoder().decode(decrypted)).toBe('Test import');
  });

  it('importPublicKeyPemForVerify imports a key for verification', async () => {
    const { signingPrivateKey, signingPublicKey } = await generateRsaKeyPair();
    const pem = await exportPublicKeyPem(signingPublicKey);
    const imported = await importPublicKeyPemForVerify(pem);
    const data = new TextEncoder().encode('Verify import');
    const signature = await rsaPssSign(signingPrivateKey, data);
    const valid = await rsaPssVerify(imported, data, signature);
    expect(valid).toBe(true);
  });
});
