export async function deriveKeyFromPassword(password: string, salt: Uint8Array): Promise<CryptoKey> {
  const encoded = new TextEncoder().encode(password);
  const keyMaterial = await crypto.subtle.importKey(
    'raw',
    encoded as Uint8Array<ArrayBuffer>,
    'PBKDF2',
    false,
    ['deriveBits'],
  );
  const derivedBits = await crypto.subtle.deriveBits(
    {
      name: 'PBKDF2',
      salt: salt as Uint8Array<ArrayBuffer>,
      iterations: 600000,
      hash: 'SHA-256',
    },
    keyMaterial,
    256,
  );
  return crypto.subtle.importKey('raw', derivedBits, { name: 'AES-GCM', length: 256 }, true, [
    'encrypt',
    'decrypt',
  ]);
}

export async function encryptPrivateKey(
  privateKeyBytes: Uint8Array,
  password: string,
): Promise<{ encrypted: string; iv: string }> {
  const salt = new Uint8Array(crypto.getRandomValues(new Uint8Array(16)));
  const key = await deriveKeyFromPassword(password, salt);
  const iv = new Uint8Array(crypto.getRandomValues(new Uint8Array(12)));
  const encrypted = await crypto.subtle.encrypt(
    { name: 'AES-GCM', iv: iv as Uint8Array<ArrayBuffer> },
    key,
    privateKeyBytes as Uint8Array<ArrayBuffer>,
  );
  const ivAndSalt = new Uint8Array(iv.length + salt.length);
  ivAndSalt.set(iv, 0);
  ivAndSalt.set(salt, iv.length);
  return {
    encrypted: arrayBufferToBase64(encrypted),
    iv: arrayBufferToBase64(ivAndSalt.buffer as ArrayBuffer),
  };
}

export async function decryptPrivateKey(
  encryptedB64: string,
  ivSaltB64: string,
  password: string,
): Promise<Uint8Array> {
  const ivSalt = base64ToArrayBuffer(ivSaltB64);
  const iv = ivSalt.slice(0, 12);
  const salt = ivSalt.slice(12);
  const key = await deriveKeyFromPassword(password, salt);
  const ciphertext = base64ToArrayBuffer(encryptedB64);
  const decrypted = await crypto.subtle.decrypt(
    { name: 'AES-GCM', iv: iv as Uint8Array<ArrayBuffer> },
    key,
    ciphertext as Uint8Array<ArrayBuffer>,
  );
  return new Uint8Array(decrypted);
}

export function generateNonce(length: number): Uint8Array {
  return crypto.getRandomValues(new Uint8Array(length));
}

function arrayBufferToBase64(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]!);
  }
  return btoa(binary);
}

function base64ToArrayBuffer(base64: string): Uint8Array {
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}
