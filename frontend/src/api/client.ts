const API_BASE = '/api/v1';

let authToken: string | null = null;
let onUnauthorizedCallback: (() => void) | null = null;

export function setAuthToken(token: string | null): void {
  authToken = token;
}

export function getAuthToken(): string | null {
  return authToken;
}

export function setOnUnauthorized(callback: () => void): void {
  onUnauthorizedCallback = callback;
}

export async function apiFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  if (authToken) {
    headers['Authorization'] = `Bearer ${authToken}`;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      ...headers,
      ...(options?.headers as Record<string, string>),
    },
  });

  if (response.status === 401) {
    if (onUnauthorizedCallback) {
      onUnauthorizedCallback();
    }
    throw new Error('Unauthorized');
  }

  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(errorBody || `Request failed with status ${response.status}`);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json();
}
