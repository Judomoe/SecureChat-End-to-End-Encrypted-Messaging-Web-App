export async function generateRsaKeyPair(): Promise<{
  encryptionKeyPair: CryptoKeyPair;
  signingPrivateKey: CryptoKey;
  signingPublicKey: CryptoKey;
}> {
  const encryptionKeyPair = (await crypto.subtle.generateKey(
    {
      name: 'RSA-OAEP',
      modulusLength: 2048,
      publicExponent: new Uint8Array([1, 0, 1]),
      hash: 'SHA-256',
    },
    true,
    ['encrypt', 'decrypt'],
  )) as CryptoKeyPair;

  const pkcs8 = await crypto.subtle.exportKey('pkcs8', encryptionKeyPair.privateKey);
  const signingPrivateKey = await crypto.subtle.importKey(
    'pkcs8',
    pkcs8,
    { name: 'RSA-PSS', hash: 'SHA-256' },
    true,
    ['sign'],
  );

  const spki = await crypto.subtle.exportKey('spki', encryptionKeyPair.publicKey);
  const signingPublicKey = await crypto.subtle.importKey(
    'spki',
    spki,
    { name: 'RSA-PSS', hash: 'SHA-256' },
    true,
    ['verify'],
  );

  return { encryptionKeyPair, signingPrivateKey, signingPublicKey };
}

export async function rsaOaepEncrypt(publicKey: CryptoKey, data: Uint8Array): Promise<Uint8Array> {
  const encrypted = await crypto.subtle.encrypt(
    { name: 'RSA-OAEP' },
    publicKey,
    data as Uint8Array<ArrayBuffer>,
  );
  return new Uint8Array(encrypted);
}

export async function rsaOaepDecrypt(privateKey: CryptoKey, data: Uint8Array): Promise<Uint8Array> {
  const decrypted = await crypto.subtle.decrypt(
    { name: 'RSA-OAEP' },
    privateKey,
    data as Uint8Array<ArrayBuffer>,
  );
  return new Uint8Array(decrypted);
}

export async function rsaPssSign(privateKey: CryptoKey, data: Uint8Array): Promise<Uint8Array> {
  const signature = await crypto.subtle.sign(
    { name: 'RSA-PSS', saltLength: 32 },
    privateKey,
    data as Uint8Array<ArrayBuffer>,
  );
  return new Uint8Array(signature);
}

export async function rsaPssVerify(
  publicKey: CryptoKey,
  data: Uint8Array,
  signature: Uint8Array,
): Promise<boolean> {
  return crypto.subtle.verify(
    { name: 'RSA-PSS', saltLength: 32 },
    publicKey,
    signature as Uint8Array<ArrayBuffer>,
    data as Uint8Array<ArrayBuffer>,
  );
}

export async function exportPublicKeyPem(publicKey: CryptoKey): Promise<string> {
  const spki = await crypto.subtle.exportKey('spki', publicKey);
  const base64 = arrayBufferToBase64(spki);
  return `-----BEGIN PUBLIC KEY-----\n${base64}\n-----END PUBLIC KEY-----`;
}

export async function importPublicKeyPem(pem: string): Promise<CryptoKey> {
  const base64 = pem
    .replace(/-----BEGIN PUBLIC KEY-----/, '')
    .replace(/-----END PUBLIC KEY-----/, '')
    .replace(/\s/g, '');
  const buffer = base64ToArrayBuffer(base64);
  return crypto.subtle.importKey(
    'spki',
    buffer as Uint8Array<ArrayBuffer>,
    { name: 'RSA-OAEP', hash: 'SHA-256' },
    true,
    ['encrypt'],
  );
}

export async function importPublicKeyPemForVerify(pem: string): Promise<CryptoKey> {
  const base64 = pem
    .replace(/-----BEGIN PUBLIC KEY-----/, '')
    .replace(/-----END PUBLIC KEY-----/, '')
    .replace(/\s/g, '');
  const buffer = base64ToArrayBuffer(base64);
  return crypto.subtle.importKey(
    'spki',
    buffer as Uint8Array<ArrayBuffer>,
    { name: 'RSA-PSS', hash: 'SHA-256' },
    true,
    ['verify'],
  );
}

export async function exportPrivateKeyPkcs8(privateKey: CryptoKey): Promise<Uint8Array> {
  const pkcs8 = await crypto.subtle.exportKey('pkcs8', privateKey);
  return new Uint8Array(pkcs8);
}

export async function importPrivateKeyPkcs8(pkcs8: Uint8Array, algorithm: string): Promise<CryptoKey> {
  if (algorithm === 'RSA-OAEP') {
    return crypto.subtle.importKey(
      'pkcs8',
      pkcs8 as Uint8Array<ArrayBuffer>,
      { name: 'RSA-OAEP', hash: 'SHA-256' },
      true,
      ['decrypt'],
    );
  }
  if (algorithm === 'RSA-PSS') {
    return crypto.subtle.importKey(
      'pkcs8',
      pkcs8 as Uint8Array<ArrayBuffer>,
      { name: 'RSA-PSS', hash: 'SHA-256' },
      true,
      ['sign'],
    );
  }
  throw new Error(`Unsupported algorithm: ${algorithm}`);
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
