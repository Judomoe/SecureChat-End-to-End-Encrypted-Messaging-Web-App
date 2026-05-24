export async function generateAesKey(): Promise<CryptoKey> {
  return crypto.subtle.generateKey({ name: 'AES-GCM', length: 256 }, true, [
    'encrypt',
    'decrypt',
  ]);
}

export async function aesGcmEncrypt(
  key: CryptoKey,
  plaintext: string,
): Promise<{ ciphertext: string; iv: string }> {
  const iv = new Uint8Array(crypto.getRandomValues(new Uint8Array(12)));
  const encoded = new TextEncoder().encode(plaintext);
  const encrypted = await crypto.subtle.encrypt(
    { name: 'AES-GCM', iv: iv as Uint8Array<ArrayBuffer> },
    key,
    encoded as Uint8Array<ArrayBuffer>,
  );
  return {
    ciphertext: arrayBufferToBase64(encrypted),
    iv: arrayBufferToBase64(iv.buffer as ArrayBuffer),
  };
}

export async function aesGcmDecrypt(
  key: CryptoKey,
  ciphertextB64: string,
  ivB64: string,
): Promise<string> {
  const ciphertext = base64ToArrayBuffer(ciphertextB64);
  const iv = base64ToArrayBuffer(ivB64);
  const decrypted = await crypto.subtle.decrypt(
    { name: 'AES-GCM', iv: iv as Uint8Array<ArrayBuffer> },
    key,
    ciphertext as Uint8Array<ArrayBuffer>,
  );
  return new TextDecoder().decode(decrypted);
}

export async function exportRawKey(key: CryptoKey): Promise<Uint8Array> {
  const raw = await crypto.subtle.exportKey('raw', key);
  return new Uint8Array(raw);
}

export function arrayBufferToBase64(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]!);
  }
  return btoa(binary);
}

export function base64ToArrayBuffer(base64: string): Uint8Array {
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}
