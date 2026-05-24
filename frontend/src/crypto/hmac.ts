export async function importHmacKey(rawKeyBytes: Uint8Array): Promise<CryptoKey> {
  return crypto.subtle.importKey(
    'raw',
    rawKeyBytes as Uint8Array<ArrayBuffer>,
    { name: 'HMAC', hash: 'SHA-256' },
    true,
    ['sign', 'verify'],
  );
}

export async function hmacSha256(key: CryptoKey, message: string): Promise<string> {
  const encoded = new TextEncoder().encode(message);
  const signature = await crypto.subtle.sign(
    'HMAC',
    key,
    encoded as Uint8Array<ArrayBuffer>,
  );
  return arrayBufferToBase64(signature);
}

export async function hmacVerify(key: CryptoKey, message: string, expectedHmacB64: string): Promise<boolean> {
  const encoded = new TextEncoder().encode(message);
  const expected = base64ToArrayBuffer(expectedHmacB64);
  return crypto.subtle.verify(
    'HMAC',
    key,
    expected as Uint8Array<ArrayBuffer>,
    encoded as Uint8Array<ArrayBuffer>,
  );
}

export async function exportRawKey(key: CryptoKey): Promise<Uint8Array> {
  const raw = await crypto.subtle.exportKey('raw', key);
  return new Uint8Array(raw);
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
