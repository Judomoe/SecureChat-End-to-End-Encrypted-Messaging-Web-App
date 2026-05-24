import type { Contact } from '../types';

interface NewConversationModalProps {
  contacts: Contact[];
  onSelect: (recipientId: number) => void;
  onClose: () => void;
}

export default function NewConversationModal({
  contacts,
  onSelect,
  onClose,
}: NewConversationModalProps) {
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>Start New Chat</h3>
        {contacts.length === 0 && <p>No contacts available</p>}
        <ul className="modal-list">
          {contacts.map((c) => (
            <li key={c.id} className="modal-list-item">
              <span>{c.otherUser.displayName || c.otherUser.username}</span>
              <button onClick={() => onSelect(c.otherUser.id)}>Start Chat</button>
            </li>
          ))}
        </ul>
        <button className="modal-close" onClick={onClose}>
          Cancel
        </button>
      </div>
    </div>
  );
}
