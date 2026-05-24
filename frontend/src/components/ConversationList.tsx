import { useState, useEffect, useRef } from 'react';
import type { Conversation } from '../types';
import { useConversations } from '../hooks/useConversations';
import NewConversationModal from './NewConversationModal';
import { useContacts } from '../hooks/useContacts';
import { useAuth } from '../context/AuthContext';

interface ConversationListProps {
  activeConversationId: number | null;
  onSelectConversation: (conv: Conversation) => void;
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

function formatTime(ts: number | string): string {
  const date = new Date(ts);
  const now = new Date();
  const isToday = date.toDateString() === now.toDateString();
  const yesterday = new Date(now);
  yesterday.setDate(yesterday.getDate() - 1);
  const isYesterday = date.toDateString() === yesterday.toDateString();

  const hours = date.getHours();
  const minutes = date.getMinutes().toString().padStart(2, '0');
  const ampm = hours >= 12 ? 'PM' : 'AM';
  const hour12 = hours % 12 || 12;
  const time = `${hour12}:${minutes} ${ampm}`;

  if (isToday) return time;
  if (isYesterday) return 'Yesterday';
  return date.toLocaleDateString();
}

export default function ConversationList({
  activeConversationId,
  onSelectConversation,
}: ConversationListProps) {
  const { conversations, createConversation, loadConversations, selectConversation } =
    useConversations();
  const { contacts } = useContacts();
  const { user } = useAuth();
  const [showModal, setShowModal] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    loadConversations();
  }, [loadConversations]);

  const handleSelect = async (conv: Conversation) => {
    await selectConversation(conv);
    onSelectConversation(conv);
  };

  const handleCreate = async (recipientId: number) => {
    setShowModal(false);
    const conv = await createConversation(recipientId);
    onSelectConversation(conv);
  };

  const getOtherMember = (conv: Conversation) => {
    return conv.members.find((m) => m.userId !== user?.id) || conv.members[0];
  };

  const sorted = [...conversations].sort((a, b) => {
    const aTs = a.lastMessage?.timestamp;
    const bTs = b.lastMessage?.timestamp;
    if (aTs && bTs) return bTs - aTs;
    if (aTs) return -1;
    if (bTs) return 1;
    return (b.id || 0) - (a.id || 0);
  });

  return (
    <div className="conversation-list" ref={scrollRef}>
      <div className="conversation-list-header">
        <button className="new-chat-btn" onClick={() => setShowModal(true)}>
          New Chat
        </button>
      </div>
      {sorted.length === 0 && (
        <p className="empty-text">No conversations yet</p>
      )}
      {sorted.map((conv) => {
        const other = getOtherMember(conv);
        const name = other?.username || 'Unknown';
        const initials = getInitials(name);
        const lastMsg = conv.lastMessage;
        const previewLabel = lastMsg
          ? lastMsg.senderId === user?.id
            ? 'You: '
            : ''
          : 'Tap to chat';
        const ts = lastMsg?.timestamp;

        return (
          <div
            key={conv.id}
            className={`conversation-item ${conv.id === activeConversationId ? 'active' : ''}`}
            onClick={() => handleSelect(conv)}
          >
            <div className="avatar">{initials || '?'}</div>
            <div className="conversation-info">
              <div className="conversation-name">{name}</div>
              <div className="conversation-preview">
                {ts ? previewLabel : 'Tap to chat'}
              </div>
            </div>
            <div className="conversation-meta">
              {ts && <span className="conversation-time">{formatTime(ts)}</span>}
            </div>
          </div>
        );
      })}
      {showModal && (
        <NewConversationModal
          contacts={contacts}
          onSelect={handleCreate}
          onClose={() => setShowModal(false)}
        />
      )}
    </div>
  );
}
