import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { useConversations } from '../hooks/useConversations';
import ContactList from './ContactList';
import ConversationList from './ConversationList';
import ChatWindow from './ChatWindow';

function getInitials(name: string): string {
  return name
    .split(/[\s_]+/)
    .filter(Boolean)
    .map((w) => w[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
}

export default function Dashboard() {
  const [activeConversationId, setActiveConversationId] = useState<number | null>(null);
  const [showContacts, setShowContacts] = useState(false);
  const { user, logout } = useAuth();
  const { conversations, createConversation, selectConversation, loadConversations } = useConversations();

  useEffect(() => {
    loadConversations();
  }, [loadConversations]);

  const handleStartConversation = async (recipientId: number) => {
    const existing = conversations.find((c) =>
      c.members.some((m) => m.userId === recipientId)
    );
    if (existing) {
      setShowContacts(false);
      setActiveConversationId(existing.id);
      selectConversation(existing);
      return;
    }
    const conv = await createConversation(recipientId);
    if (conv) {
      setShowContacts(false);
      setActiveConversationId(conv.id);
    }
  };

  return (
    <div className="dashboard">
      <div className="sidebar">
        <div className="sidebar-header">
          <div className="user-avatar">
            {getInitials(user?.displayName || user?.username || '?')}
          </div>
          <div className="user-info">
            <span>{user?.displayName || user?.username}</span>
            <button className="logout-btn" onClick={logout}>
              Logout
            </button>
          </div>
        </div>
        <div className="sidebar-tabs">
          <button
            className={!showContacts ? 'active' : ''}
            onClick={() => setShowContacts(false)}
          >
            Chats
          </button>
          <button
            className={showContacts ? 'active' : ''}
            onClick={() => setShowContacts(true)}
          >
            Contacts
          </button>
        </div>
        <div className="sidebar-content">
          {showContacts ? (
            <ContactList onStartConversation={handleStartConversation} />
          ) : (
            <ConversationList
              activeConversationId={activeConversationId}
              onSelectConversation={(conv) => setActiveConversationId(conv.id)}
            />
          )}
        </div>
      </div>
      <div className="main-area">
        {activeConversationId ? (
          <ChatWindow conversationId={activeConversationId} />
        ) : (
          <div className="no-selection">
            <div className="no-selection-icon">💬</div>
            <p>Select a conversation to start chatting</p>
          </div>
        )}
      </div>
    </div>
  );
}
