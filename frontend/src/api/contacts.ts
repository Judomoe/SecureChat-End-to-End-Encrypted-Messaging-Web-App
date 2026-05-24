import type { Contact } from '../types';
import { apiFetch } from './client';

export async function sendContactRequest(recipientId: number): Promise<Contact> {
  return apiFetch('/contacts/request', {
    method: 'POST',
    body: JSON.stringify({ recipientId }),
  });
}

export async function listContacts(): Promise<Contact[]> {
  return apiFetch('/contacts');
}

export async function listPendingRequests(): Promise<Contact[]> {
  return apiFetch('/contacts/pending');
}

export async function listSentRequests(): Promise<Contact[]> {
  return apiFetch('/contacts/sent');
}

export async function acceptContactRequest(contactId: number): Promise<Contact> {
  return apiFetch(`/contacts/${contactId}/accept`, {
    method: 'POST',
  });
}

export async function removeContact(contactId: number): Promise<void> {
  return apiFetch(`/contacts/${contactId}`, {
    method: 'DELETE',
  });
}
