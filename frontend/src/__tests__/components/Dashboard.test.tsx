// @vitest-environment jsdom
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import '@testing-library/jest-dom/vitest';
import { render, screen, cleanup } from '@testing-library/react';

const mockUser = {
  id: 1,
  username: 'testuser',
  email: 'test@example.com',
  displayName: 'Test User',
  createdAt: '2024-01-01T00:00:00Z',
};

const mocks = vi.hoisted(() => ({
  mockLogout: vi.fn(),
}));

vi.mock('../../context/AuthContext', () => ({
  useAuth: () => ({
    user: mockUser,
    logout: mocks.mockLogout,
    token: 'token',
    privateKey: null,
    signingKey: null,
    conversationKeys: new Map(),
    loading: false,
    error: null,
    login: vi.fn(),
    register: vi.fn(),
    setConversationKey: vi.fn(),
    getConversationKey: vi.fn(),
  }),
}));

vi.mock('../../components/ContactList', () => ({
  default: () => null,
}));

vi.mock('../../components/ConversationList', () => ({
  default: () => null,
}));

vi.mock('../../components/ChatWindow', () => ({
  default: () => null,
}));

import Dashboard from '../../components/Dashboard';

describe('Dashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it('renders sidebar and main area', () => {
    const { container } = render(<Dashboard />);
    expect(container.querySelector('.sidebar')).toBeInTheDocument();
    expect(container.querySelector('.main-area')).toBeInTheDocument();
  });

  it('shows user info', () => {
    render(<Dashboard />);
    expect(screen.getByText('Test User')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /logout/i })).toBeInTheDocument();
  });
});
