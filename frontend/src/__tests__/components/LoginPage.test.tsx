// @vitest-environment jsdom
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import '@testing-library/jest-dom/vitest';
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const mocks = vi.hoisted(() => ({
  mockLogin: vi.fn(),
  mockNavigate: vi.fn(),
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mocks.mockNavigate,
  };
});

vi.mock('../../context/AuthContext', () => ({
  useAuth: () => ({
    login: mocks.mockLogin,
    token: null,
    user: null,
    privateKey: null,
    signingKey: null,
    conversationKeys: new Map(),
    loading: false,
    error: null,
    register: vi.fn(),
    logout: vi.fn(),
    setConversationKey: vi.fn(),
    getConversationKey: vi.fn(),
  }),
}));

import LoginPage from '../../components/LoginPage';

function renderLoginPage() {
  return render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>,
  );
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it('renders email and password inputs', () => {
    renderLoginPage();
    expect(screen.getByLabelText('Email')).toBeInTheDocument();
    expect(screen.getByLabelText('Password')).toBeInTheDocument();
  });

  it('submit calls login function', async () => {
    mocks.mockLogin.mockResolvedValue(undefined);
    renderLoginPage();

    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'test@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password123' } });
    fireEvent.click(screen.getByRole('button', { name: /login/i }));

    await waitFor(() => {
      expect(mocks.mockLogin).toHaveBeenCalledWith('test@example.com', 'password123');
    });
  });

  it('shows error on failed login', async () => {
    mocks.mockLogin.mockRejectedValue(new Error('Invalid credentials'));
    renderLoginPage();

    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'bad@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'wrong' } });
    fireEvent.click(screen.getByRole('button', { name: /login/i }));

    await waitFor(() => {
      expect(screen.getByText('Invalid credentials')).toBeInTheDocument();
    });
  });
});
