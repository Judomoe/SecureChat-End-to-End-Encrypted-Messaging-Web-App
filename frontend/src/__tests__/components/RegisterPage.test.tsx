// @vitest-environment jsdom
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import '@testing-library/jest-dom/vitest';
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const mocks = vi.hoisted(() => ({
  mockRegister: vi.fn(),
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
    register: mocks.mockRegister,
    token: null,
    user: null,
    privateKey: null,
    signingKey: null,
    conversationKeys: new Map(),
    loading: false,
    error: null,
    login: vi.fn(),
    logout: vi.fn(),
    setConversationKey: vi.fn(),
    getConversationKey: vi.fn(),
  }),
}));

import RegisterPage from '../../components/RegisterPage';

function renderRegisterPage() {
  return render(
    <MemoryRouter>
      <RegisterPage />
    </MemoryRouter>,
  );
}

describe('RegisterPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it('renders registration form', () => {
    renderRegisterPage();
    expect(screen.getByLabelText('Username')).toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toBeInTheDocument();
    expect(screen.getByLabelText('Display Name')).toBeInTheDocument();
    expect(screen.getByLabelText('Password')).toBeInTheDocument();
    expect(screen.getByLabelText('Confirm Password')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /register/i })).toBeInTheDocument();
  });

  it('submit calls register function', async () => {
    mocks.mockRegister.mockResolvedValue(undefined);
    renderRegisterPage();

    fireEvent.change(screen.getByLabelText('Username'), { target: { value: 'newuser' } });
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'new@example.com' } });
    fireEvent.change(screen.getByLabelText('Display Name'), { target: { value: 'New User' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password123' } });
    fireEvent.change(screen.getByLabelText('Confirm Password'), { target: { value: 'password123' } });
    fireEvent.click(screen.getByRole('button', { name: /register/i }));

    await waitFor(() => {
      expect(mocks.mockRegister).toHaveBeenCalledWith('newuser', 'new@example.com', 'password123', 'New User');
    });
  });
});
