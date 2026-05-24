import { createContext, useContext, useState, useCallback, useEffect, type ReactNode } from 'react';
import type { User, LoginResponse } from '../types';
import * as authApi from '../api/auth';
import * as rsa from '../crypto/rsa';
import * as keyManagement from '../crypto/keyManagement';
import { setAuthToken, setOnUnauthorized } from '../api/client';

interface AuthContextType {
  token: string | null;
  user: User | null;
  privateKey: CryptoKey | null;
  signingKey: CryptoKey | null;
  conversationKeys: Map<number, CryptoKey>;
  loading: boolean;
  error: string | null;
  login: (email: string, password: string) => Promise<void>;
  register: (username: string, email: string, password: string, displayName: string) => Promise<void>;
  logout: () => void;
  setConversationKey: (conversationId: number, key: CryptoKey) => void;
  getConversationKey: (conversationId: number) => CryptoKey | undefined;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<User | null>(null);
  const [privateKey, setPrivateKey] = useState<CryptoKey | null>(null);
  const [signingKey, setSigningKey] = useState<CryptoKey | null>(null);
  const [conversationKeys, setConversationKeys] = useState<Map<number, CryptoKey>>(new Map());
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const logout = useCallback(() => {
    setToken(null);
    setUser(null);
    setPrivateKey(null);
    setSigningKey(null);
    setConversationKeys(new Map());
    setError(null);
    setAuthToken(null);
  }, []);

  useEffect(() => {
    setOnUnauthorized(logout);
  }, [logout]);

  const setConversationKey = useCallback((conversationId: number, key: CryptoKey) => {
    setConversationKeys((prev) => {
      const next = new Map(prev);
      next.set(conversationId, key);
      return next;
    });
  }, []);

  const getConversationKey = useCallback(
    (conversationId: number) => {
      return conversationKeys.get(conversationId);
    },
    [conversationKeys],
  );

  const login = useCallback(
    async (email: string, password: string) => {
      setLoading(true);
      setError(null);
      try {
        const response: LoginResponse = await authApi.login(email, password);
        setToken(response.token);
        setUser(response.user);
        setAuthToken(response.token);

        const pkcs8Bytes = await keyManagement.decryptPrivateKey(
          response.encryptedPrivateKey,
          response.privateKeyIv,
          password,
        );

        const oaepPrivateKey = await rsa.importPrivateKeyPkcs8(pkcs8Bytes, 'RSA-OAEP');
        const pssPrivateKey = await rsa.importPrivateKeyPkcs8(pkcs8Bytes, 'RSA-PSS');

        setPrivateKey(oaepPrivateKey);
        setSigningKey(pssPrivateKey);
      } catch (err: unknown) {
        const message = err instanceof Error ? err.message : 'Login failed';
        setError(message);
        throw err;
      } finally {
        setLoading(false);
      }
    },
    [],
  );

  const register = useCallback(
    async (username: string, email: string, password: string, displayName: string) => {
      setLoading(true);
      setError(null);
      try {
        const { encryptionKeyPair, signingPrivateKey } = await rsa.generateRsaKeyPair();
        const publicKeyPem = await rsa.exportPublicKeyPem(encryptionKeyPair.publicKey);
        const pkcs8Bytes = await rsa.exportPrivateKeyPkcs8(encryptionKeyPair.privateKey);
        const { encrypted, iv } = await keyManagement.encryptPrivateKey(pkcs8Bytes, password);

        const response = await authApi.register({
          username,
          email,
          password,
          displayName,
          publicKeyPem,
          encryptedPrivateKey: encrypted,
          privateKeyIv: iv,
        });

        setToken((response as unknown as LoginResponse).token);
        setUser({
          id: response.id,
          username: response.username,
          email: response.email,
          displayName: response.displayName,
          createdAt: response.createdAt,
        });
        setAuthToken((response as unknown as LoginResponse).token);

        setPrivateKey(encryptionKeyPair.privateKey);
        setSigningKey(signingPrivateKey);
      } catch (err: unknown) {
        const message = err instanceof Error ? err.message : 'Registration failed';
        setError(message);
        throw err;
      } finally {
        setLoading(false);
      }
    },
    [],
  );

  return (
    <AuthContext.Provider
      value={{
        token,
        user,
        privateKey,
        signingKey,
        conversationKeys,
        loading,
        error,
        login,
        register,
        logout,
        setConversationKey,
        getConversationKey,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
