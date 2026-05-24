import type { DecryptedMessage } from '../types';

interface MessageBubbleProps {
  message: DecryptedMessage;
  currentUserId: number;
}

export default function MessageBubble({ message, currentUserId }: MessageBubbleProps) {
  const isSent = message.senderId === currentUserId;

  return (
    <div className={`message-bubble ${isSent ? 'sent' : 'received'}`}>
      <span className="message-sender">{message.senderUsername}</span>
      <p className="message-text">{message.plaintext}</p>
      <span className="message-timestamp">
        {new Date(message.timestamp).toLocaleTimeString()}
      </span>
    </div>
  );
}
