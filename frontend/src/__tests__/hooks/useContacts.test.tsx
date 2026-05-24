// @vitest-environment jsdom
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { useContacts } from '../../hooks/useContacts';

vi.mock('../../api/contacts', () => ({
  listContacts: vi.fn(),
  listPendingRequests: vi.fn(),
  listSentRequests: vi.fn(),
  sendContactRequest: vi.fn(),
  acceptContactRequest: vi.fn(),
  removeContact: vi.fn(),
}));

vi.mock('../../api/users', () => ({
  searchUsers: vi.fn().mockResolvedValue([]),
}));

import { listContacts, listPendingRequests, listSentRequests, sendContactRequest, acceptContactRequest } from '../../api/contacts';

describe('useContacts', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('initial load fetches contacts, pending, and sent requests', async () => {
    const mockContacts = [
      { id: 1, requesterId: 1, recipientId: 2, status: 'ACCEPTED' as const, otherUser: { id: 2, username: 'user2', email: 'e@e.com', displayName: null, createdAt: '' }, createdAt: '' },
    ];
    const mockPending = [
      { id: 2, requesterId: 3, recipientId: 1, status: 'PENDING' as const, otherUser: { id: 3, username: 'user3', email: 'e@e.com', displayName: null, createdAt: '' }, createdAt: '' },
    ];
    const mockSent = [
      { id: 3, requesterId: 1, recipientId: 4, status: 'PENDING' as const, otherUser: { id: 4, username: 'user4', email: 'e@e.com', displayName: null, createdAt: '' }, createdAt: '' },
    ];
    vi.mocked(listContacts).mockResolvedValue(mockContacts);
    vi.mocked(listPendingRequests).mockResolvedValue(mockPending);
    vi.mocked(listSentRequests).mockResolvedValue(mockSent);

    const { result } = renderHook(() => useContacts());

    await waitFor(() => {
      expect(result.current.contacts).toEqual(mockContacts);
      expect(result.current.pendingRequests).toEqual(mockPending);
      expect(result.current.sentRequests).toEqual(mockSent);
    });

    expect(listContacts).toHaveBeenCalled();
    expect(listPendingRequests).toHaveBeenCalled();
    expect(listSentRequests).toHaveBeenCalled();
  });

  it('sendRequest calls API', async () => {
    vi.mocked(listContacts).mockResolvedValue([]);
    vi.mocked(listPendingRequests).mockResolvedValue([]);
    vi.mocked(listSentRequests).mockResolvedValue([]);
    vi.mocked(sendContactRequest).mockResolvedValue({} as never);

    const { result } = renderHook(() => useContacts());

    await waitFor(() => expect(result.current.contacts).toBeDefined());

    await result.current.sendRequest(5);

    expect(sendContactRequest).toHaveBeenCalledWith(5);
  });

  it('acceptRequest calls API', async () => {
    vi.mocked(listContacts).mockResolvedValue([]);
    vi.mocked(listPendingRequests).mockResolvedValue([]);
    vi.mocked(listSentRequests).mockResolvedValue([]);
    vi.mocked(acceptContactRequest).mockResolvedValue({} as never);

    const { result } = renderHook(() => useContacts());

    await waitFor(() => expect(result.current.contacts).toBeDefined());

    await result.current.acceptRequest(3);

    expect(acceptContactRequest).toHaveBeenCalledWith(3);
  });
});
