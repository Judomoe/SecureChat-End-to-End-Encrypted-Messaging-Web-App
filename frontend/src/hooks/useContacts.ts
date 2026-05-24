import { useState, useEffect, useCallback } from 'react';
import type { Contact, User } from '../types';
import * as contactsApi from '../api/contacts';
import * as usersApi from '../api/users';

export function useContacts() {
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [pendingRequests, setPendingRequests] = useState<Contact[]>([]);
  const [sentRequests, setSentRequests] = useState<Contact[]>([]);
  const [searchResults, setSearchResults] = useState<User[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(false);

  const loadContacts = useCallback(async () => {
    try {
      const [accepted, pending, sent] = await Promise.all([
        contactsApi.listContacts(),
        contactsApi.listPendingRequests(),
        contactsApi.listSentRequests(),
      ]);
      setContacts(accepted);
      setPendingRequests(pending);
      setSentRequests(sent);
    } catch {
      // ignore
    }
  }, []);

  useEffect(() => {
    loadContacts();
  }, [loadContacts]);

  const searchUsers = useCallback(async (query: string) => {
    setSearchQuery(query);
    if (!query.trim()) {
      setSearchResults([]);
      return;
    }
    try {
      const results = await usersApi.searchUsers(query);
      setSearchResults(results);
    } catch {
      setSearchResults([]);
    }
  }, []);

  const sendRequest = useCallback(
    async (recipientId: number) => {
      setLoading(true);
      try {
        await contactsApi.sendContactRequest(recipientId);
        await loadContacts();
      } finally {
        setLoading(false);
      }
    },
    [loadContacts],
  );

  const acceptRequest = useCallback(
    async (contactId: number) => {
      setLoading(true);
      try {
        await contactsApi.acceptContactRequest(contactId);
        await loadContacts();
      } finally {
        setLoading(false);
      }
    },
    [loadContacts],
  );

  const removeContact = useCallback(
    async (contactId: number) => {
      setLoading(true);
      try {
        await contactsApi.removeContact(contactId);
        await loadContacts();
      } finally {
        setLoading(false);
      }
    },
    [loadContacts],
  );

  return {
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
    loadContacts,
  };
}
