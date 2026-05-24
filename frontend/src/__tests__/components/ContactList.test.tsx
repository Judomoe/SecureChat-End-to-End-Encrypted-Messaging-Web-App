// @vitest-environment jsdom
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import '@testing-library/jest-dom/vitest';
import { render, screen, fireEvent, cleanup } from '@testing-library/react';

const mockContacts = [
  { id: 1, requesterId: 1, recipientId: 2, status: 'ACCEPTED' as const, otherUser: { id: 2, username: 'alice', email: 'a@a.com', displayName: 'Alice', createdAt: '' }, createdAt: '' },
];

const mocks = vi.hoisted(() => ({
  searchUsers: vi.fn(),
  sendRequest: vi.fn(),
  acceptRequest: vi.fn(),
  removeContact: vi.fn(),
  loadContacts: vi.fn(),
  onStartConversation: vi.fn(),
}));

vi.mock('../../hooks/useContacts', () => ({
  useContacts: vi.fn(),
}));

vi.mock('../../context/AuthContext', () => ({
  useAuth: vi.fn(),
}));

import ContactList from '../../components/ContactList';

import { useContacts } from '../../hooks/useContacts';
import { useAuth } from '../../context/AuthContext';
const mockedUseContacts = vi.mocked(useContacts);
const mockedUseAuth = vi.mocked(useAuth);

describe('ContactList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedUseContacts.mockReturnValue({
      contacts: [],
      pendingRequests: [],
      sentRequests: [],
      searchResults: [],
      searchQuery: '',
      loading: false,
      searchUsers: mocks.searchUsers,
      sendRequest: mocks.sendRequest,
      acceptRequest: mocks.acceptRequest,
      removeContact: mocks.removeContact,
      loadContacts: mocks.loadContacts,
    });
    mockedUseAuth.mockReturnValue({
      user: { id: 99, username: 'me', email: 'me@me.com', displayName: 'Me', createdAt: '' },
      token: 'test',
      privateKey: null as any,
      signingKey: null as any,
      conversationKeys: new Map(),
      loading: false,
      error: null,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      setConversationKey: vi.fn(),
      getConversationKey: vi.fn(),
    });
  });

  afterEach(() => {
    cleanup();
  });

  it('renders contacts list', () => {
    mockedUseContacts.mockReturnValue({
      contacts: mockContacts,
      pendingRequests: [],
      sentRequests: [],
      searchResults: [],
      searchQuery: '',
      loading: false,
      searchUsers: mocks.searchUsers,
      sendRequest: mocks.sendRequest,
      acceptRequest: mocks.acceptRequest,
      removeContact: mocks.removeContact,
      loadContacts: mocks.loadContacts,
    });

    render(<ContactList />);
    expect(screen.getByText('Alice')).toBeInTheDocument();
    expect(screen.getByText('Contacts')).toBeInTheDocument();
  });

  it('search input works', () => {
    render(<ContactList />);
    const input = screen.getByPlaceholderText('Search or start new chat');
    fireEvent.change(input, { target: { value: 'alice' } });
    expect(mocks.searchUsers).toHaveBeenCalledWith('alice');
  });

  it('filters out current user, accepted contacts, pending requests, and sent requests from search results', () => {
    const mockSearchResults = [
      { id: 99, username: 'me', email: 'me@me.com', displayName: 'Me', createdAt: '' }, // Should be filtered (current user)
      { id: 2, username: 'alice', email: 'a@a.com', displayName: 'Alice', createdAt: '' }, // Should be filtered (accepted)
      { id: 3, username: 'bob', email: 'b@b.com', displayName: 'Bob', createdAt: '' }, // Should be filtered (pending)
      { id: 5, username: 'dave', email: 'd@d.com', displayName: 'Dave', createdAt: '' }, // Should be filtered (sent)
      { id: 4, username: 'charlie', email: 'c@c.com', displayName: 'Charlie', createdAt: '' }, // Should be visible
    ];

    mockedUseContacts.mockReturnValue({
      contacts: [{ id: 1, requesterId: 99, recipientId: 2, status: 'ACCEPTED', otherUser: { id: 2, username: 'alice', email: 'a@a.com', displayName: 'Alice', createdAt: '' }, createdAt: '' }],
      pendingRequests: [{ id: 2, requesterId: 99, recipientId: 3, status: 'PENDING', otherUser: { id: 3, username: 'bob', email: 'b@b.com', displayName: 'Bob', createdAt: '' }, createdAt: '' }],
      sentRequests: [{ id: 3, requesterId: 99, recipientId: 5, status: 'PENDING', otherUser: { id: 5, username: 'dave', email: 'd@d.com', displayName: 'Dave', createdAt: '' }, createdAt: '' }],
      searchResults: mockSearchResults,
      searchQuery: 'a',
      loading: false,
      searchUsers: mocks.searchUsers,
      sendRequest: mocks.sendRequest,
      acceptRequest: mocks.acceptRequest,
      removeContact: mocks.removeContact,
      loadContacts: mocks.loadContacts,
    });

    render(<ContactList />);
    
    // Charlie should be visible
    expect(screen.getByText('Charlie')).toBeInTheDocument();
    
    // Me, Alice, Bob, and Dave should NOT be in the search results
    const searchResultsContainer = screen.getByText('Search Results').parentElement;
    expect(searchResultsContainer).toHaveTextContent('Charlie');
    expect(searchResultsContainer).not.toHaveTextContent('Me');
    expect(searchResultsContainer).not.toHaveTextContent('Alice');
    expect(searchResultsContainer).not.toHaveTextContent('Bob');
    expect(searchResultsContainer).not.toHaveTextContent('Dave');
  });

  it('calls onStartConversation with contact userId when clicking a contact', () => {
    mockedUseContacts.mockReturnValue({
      contacts: mockContacts,
      pendingRequests: [],
      sentRequests: [],
      searchResults: [],
      searchQuery: '',
      loading: false,
      searchUsers: mocks.searchUsers,
      sendRequest: mocks.sendRequest,
      acceptRequest: mocks.acceptRequest,
      removeContact: mocks.removeContact,
      loadContacts: mocks.loadContacts,
    });

    mocks.onStartConversation.mockReset();
    render(<ContactList onStartConversation={mocks.onStartConversation} />);

    fireEvent.click(screen.getByText('Alice'));
    expect(mocks.onStartConversation).toHaveBeenCalledWith(2);
  });
});
