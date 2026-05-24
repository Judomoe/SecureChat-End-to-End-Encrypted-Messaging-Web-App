// @vitest-environment jsdom
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import '@testing-library/jest-dom/vitest';
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';

Element.prototype.scrollIntoView = vi.fn();

const mockMessages = [
  {
    id: 1,
    senderId: 1,
    ciphertext: 'ct',
    iv: 'iv',
    hmac: 'h',
    signature: 's',
    timestamp: Date.now(),
    plaintext: 'Hello!',
    senderUsername: 'testuser',
  },
  {
    id: 2,
    senderId: 2,
    ciphertext: 'ct2',
    iv: 'iv2',
    hmac: 'h2',
    signature: 's2',
    timestamp: Date.now(),
    plaintext: 'Hi there!',
    senderUsername: 'otheruser',
  },
];

const mocks = vi.hoisted(() => ({
  sendMessage: vi.fn(),
}));

vi.mock('../../hooks/useMessages', () => ({
  useMessages: vi.fn(),
}));

vi.mock('../../context/AuthContext', () => ({
  useAuth: () => ({
    user: { id: 1, username: 'testuser', email: 't@t.com', displayName: 'Test', createdAt: '' },
  }),
}));

import ChatWindow from '../../components/ChatWindow';

import { useMessages } from '../../hooks/useMessages';
const mockedUseMessages = vi.mocked(useMessages);

describe('ChatWindow', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    Element.prototype.scrollIntoView = vi.fn();
    mockedUseMessages.mockReturnValue({
      messages: [] as never[],
      sendMessage: mocks.sendMessage,
      loading: false,
      partnerName: 'otheruser',
      sendError: null,
    });
  });

  afterEach(() => {
    cleanup();
  });

  it('renders messages', () => {
    mockedUseMessages.mockReturnValue({
      messages: mockMessages as never[],
      sendMessage: mocks.sendMessage,
      loading: false,
      partnerName: 'otheruser',
      sendError: null,
    });

    render(<ChatWindow conversationId={1} />);
    expect(screen.getByText('Hello!')).toBeInTheDocument();
    expect(screen.getByText('Hi there!')).toBeInTheDocument();
  });

  it('send button works', async () => {
    render(<ChatWindow conversationId={1} />);
    const input = screen.getByPlaceholderText('Type a message...');
    fireEvent.change(input, { target: { value: 'New message' } });
    fireEvent.click(screen.getByRole('button', { name: 'Send' }));

    await waitFor(() => {
      expect(mocks.sendMessage).toHaveBeenCalledWith('New message');
    });
  });

  it('shows no-selection when conversationId is null', () => {
    render(<ChatWindow conversationId={null} />);
    expect(screen.getByText('Select a conversation to start chatting')).toBeInTheDocument();
  });
});
