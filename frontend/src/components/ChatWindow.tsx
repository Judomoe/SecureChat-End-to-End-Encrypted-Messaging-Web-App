import { useState, type FormEvent, useEffect, useRef } from 'react';
import { useMessages } from '../hooks/useMessages';
import { useAuth } from '../context/AuthContext';
import MessageBubble from './MessageBubble';

interface ChatWindowProps {
  conversationId: number | null;
}

function getInitials(name: string): string {
  return name
    .split(/[\s_]+/)
    .filter(Boolean)
    .map((w) => w[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
}

export default function ChatWindow({ conversationId }: ChatWindowProps) {
  const { messages, sendMessage, loading, partnerName, sendError } = useMessages(conversationId);
  const { user } = useAuth();
  const [inputText, setInputText] = useState('');
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, sendError]);

  const handleSend = (e: FormEvent) => {
    e.preventDefault();
    const text = inputText.trim();
    if (!text) return;
    sendMessage(text);
    setInputText('');
  };

  if (!conversationId) {
    return (
      <div className="no-selection">
        <div className="no-selection-icon">💬</div>
        <p>Select a conversation to start chatting</p>
      </div>
    );
  }

  return (
    <div className="chat-area">
      <div className="chat-header">
        <div className="avatar small">
          {partnerName ? getInitials(partnerName) : '?'}
        </div>
        <span className="chat-partner-name">{partnerName || 'Conversation'}</span>
      </div>
      <div className="messages-container">
        {loading && <p className="loading-text">Loading messages...</p>}
        {messages.map((msg) => (
          <MessageBubble key={msg.id} message={msg} currentUserId={user!.id} />
        ))}
        {sendError && (
          <div className="message-bubble sent">
            <span className="message-error-text">Error: {sendError}</span>
          </div>
        )}
        <div ref={bottomRef} />
      </div>
      <form className="chat-input" onSubmit={handleSend}>
        <input
          type="text"
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
          placeholder="Type a message..."
        />
        <button type="submit">Send</button>
      </form>
    </div>
  );
}
