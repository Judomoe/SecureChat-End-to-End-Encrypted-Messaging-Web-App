export interface User {
  id: number;
  username: string;
  email: string;
  displayName: string | null;
  createdAt: string;
}

export interface AuthState {
  token: string | null;
  user: User | null;
  privateKey: CryptoKey | null;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  displayName: string;
  publicKeyPem: string;
  encryptedPrivateKey: string;
  privateKeyIv: string;
}

export interface LoginResponse {
  token: string;
  user: User;
  encryptedPrivateKey: string;
  privateKeyIv: string;
}

export interface Contact {
  id: number;
  requesterId: number;
  recipientId: number;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  user: User;
  createdAt: string;
}

export interface LastMessageInfo {
  senderId: number;
  timestamp: number;
}

export interface Conversation {
  id: number;
  members: ConversationMember[];
  createdAt: string;
  lastMessage?: LastMessageInfo;
}

export interface ConversationMember {
  id: number;
  userId: number;
  username: string;
  joinedAt: string;
}

export interface EncryptedMessage {
  id: number;
  senderId: number;
  ciphertext: string;
  iv: string;
  hmac: string;
  signature: string;
  timestamp: number;
}

export interface DecryptedMessage extends EncryptedMessage {
  plaintext: string;
  senderUsername: string;
}

export interface KeyMaterial {
  id: number;
  encryptedAesKey: string;
  signature: string;
  senderId: number;
  issuedAt: string;
}

export interface ConversationKey {
  conversationId: number;
  key: CryptoKey;
}
