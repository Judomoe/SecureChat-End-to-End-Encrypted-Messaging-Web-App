import { useState } from 'react';
import { useContacts } from '../hooks/useContacts';
import { useAuth } from '../context/AuthContext';

interface ContactListProps {
  onStartConversation?: (recipientId: number) => void;
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

export default function ContactList({ onStartConversation }: ContactListProps) {
  const {
    contacts,
    pendingRequests,
    sentRequests,
    searchResults,
    searchQuery,
    loading,
    searchUsers,
    sendRequest,
    acceptRequest,
    removeContact,
  } = useContacts();
  const { user } = useAuth();
  const [query, setQuery] = useState('');

  const handleSearch = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setQuery(value);
    searchUsers(value);
  };

  return (
    <div className="contact-list">
      <div className="search-bar">
        <input
          type="text"
          placeholder="Search or start new chat"
          value={query}
          onChange={handleSearch}
        />
      </div>

      {searchQuery && searchResults.length > 0 && (
        <div className="search-results">
          <h4>Search Results</h4>
          {searchResults
            .filter((u) => u.id !== user?.id)
            .filter((u) => !contacts.some((c) => c.otherUser.id === u.id))
            .filter((u) => !pendingRequests.some((c) => c.otherUser.id === u.id))
            .filter((u) => !sentRequests.some((c) => c.otherUser.id === u.id))
            .map((u) => {
              const display = u.displayName || u.username;
              return (
                <div key={u.id} className="contact-item">
                  <div className="avatar small">{getInitials(display)}</div>
                  <div className="contact-name">
                    {display}
                    <div style={{ fontSize: '0.75rem', color: '#8696a0' }}>{u.email}</div>
                  </div>
                  <button
                    className="btn-sm btn-add"
                    onClick={() => sendRequest(u.id)}
                    disabled={loading}
                  >
                    Add
                  </button>
                </div>
              );
            })}
        </div>
      )}

      {pendingRequests.length > 0 && (
        <div className="pending-requests">
          <h4>Pending Requests</h4>
          {pendingRequests.map((c) => {
            const display = c.otherUser.displayName || c.otherUser.username;
            return (
              <div key={c.id} className="contact-item">
                <div className="avatar small">{getInitials(display)}</div>
                <div className="contact-name">{display}</div>
                <div className="contact-actions">
                  <button
                    className="btn-sm btn-accept"
                    onClick={() => acceptRequest(c.id)}
                    disabled={loading}
                  >
                    Accept
                  </button>
                  <button
                    className="btn-sm btn-danger"
                    onClick={() => removeContact(c.id)}
                    disabled={loading}
                  >
                    Reject
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {sentRequests.length > 0 && (
        <div className="sent-requests">
          <h4>Sent Requests</h4>
          {sentRequests.map((c) => {
            const display = c.otherUser.displayName || c.otherUser.username;
            return (
              <div key={c.id} className="contact-item">
                <div className="avatar small">{getInitials(display)}</div>
                <div className="contact-name">{display}</div>
                <div className="contact-actions">
                  <button
                    className="btn-sm btn-danger"
                    onClick={() => removeContact(c.id)}
                    disabled={loading}
                  >
                    Cancel
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      <div className="accepted-contacts">
        <h4>Contacts</h4>
        {contacts.length === 0 && <p className="empty-text">No contacts yet</p>}
        {contacts.map((c) => {
          const display = c.otherUser.displayName || c.otherUser.username;
          return (
            <div
              key={c.id}
              className="contact-item"
              onClick={() => onStartConversation?.(c.otherUser.id)}
            >
              <div className="avatar small">{getInitials(display)}</div>
              <div className="contact-name">{display}</div>
              <button
                className="btn-sm btn-danger"
                onClick={(e) => {
                  e.stopPropagation();
                  removeContact(c.id);
                }}
                disabled={loading}
              >
                Remove
              </button>
            </div>
          );
        })}
      </div>
    </div>
  );
}
