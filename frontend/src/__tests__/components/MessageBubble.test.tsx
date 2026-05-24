// @vitest-environment jsdom
import { describe, it, expect } from 'vitest';
import '@testing-library/jest-dom/vitest';
import { render, screen } from '@testing-library/react';
import MessageBubble from '../../components/MessageBubble';
import type { DecryptedMessage } from '../../types';

const baseMessage: DecryptedMessage = {
  id: 1,
  senderId: 1,
  ciphertext: 'ct',
  iv: 'iv',
  hmac: 'h',
  signature: 's',
  timestamp: 1700000000000,
  plaintext: 'Hello World',
  senderUsername: 'testuser',
};

describe('MessageBubble', () => {
  it('renders message text', () => {
    render(<MessageBubble message={baseMessage} currentUserId={1} />);
    expect(screen.getByText('Hello World')).toBeInTheDocument();
    expect(screen.getByText('testuser')).toBeInTheDocument();
  });

  it('applies sent class for current user messages', () => {
    const { container } = render(<MessageBubble message={baseMessage} currentUserId={1} />);
    const bubble = container.querySelector('.message-bubble');
    expect(bubble).toHaveClass('sent');
    expect(bubble).not.toHaveClass('received');
  });

  it('applies received class for other user messages', () => {
    const { container } = render(<MessageBubble message={baseMessage} currentUserId={99} />);
    const bubble = container.querySelector('.message-bubble');
    expect(bubble).toHaveClass('received');
    expect(bubble).not.toHaveClass('sent');
  });
});
