package com.securechat.contact;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.securechat.exception.ResourceNotFoundException;
import com.securechat.user.UserEntity;
import com.securechat.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ContactService contactService;

    private UserEntity createUser(Long id, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        return user;
    }

    private ContactEntity createContact(Long id, Long requesterId, Long recipientId, String status) {
        ContactEntity contact = new ContactEntity();
        contact.setId(id);
        contact.setRequesterId(requesterId);
        contact.setRecipientId(recipientId);
        contact.setStatus(status);
        return contact;
    }

    @Test
    void sendRequest_succeeds() {
        UserEntity recipient = createUser(2L, "bob");
        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));
        when(contactRepository.existsByRequesterIdAndRecipientId(1L, 2L)).thenReturn(false);
        when(contactRepository.existsByRequesterIdAndRecipientId(2L, 1L)).thenReturn(false);
        when(contactRepository.save(any(ContactEntity.class))).thenAnswer(invocation -> {
            ContactEntity c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });

        ContactEntity result = contactService.sendRequest(1L, 2L);

        assertNotNull(result);
        assertEquals(1L, result.getRequesterId());
        assertEquals(2L, result.getRecipientId());
        assertEquals("PENDING", result.getStatus());
    }

    @Test
    void sendRequest_toSelf_throwsException() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> contactService.sendRequest(1L, 1L));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void sendRequest_toNonExistentUser_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> contactService.sendRequest(1L, 999L));
    }

    @Test
    void sendRequest_duplicate_throwsConflict() {
        UserEntity recipient = createUser(2L, "bob");
        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));
        when(contactRepository.existsByRequesterIdAndRecipientId(1L, 2L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> contactService.sendRequest(1L, 2L));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void acceptRequest_succeeds() {
        ContactEntity contact = createContact(1L, 1L, 2L, "PENDING");
        when(contactRepository.findById(1L)).thenReturn(Optional.of(contact));
        when(contactRepository.save(any(ContactEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContactEntity result = contactService.acceptRequest(1L, 2L);

        assertEquals("ACCEPTED", result.getStatus());
    }

    @Test
    void acceptRequest_byWrongUser_throwsForbidden() {
        ContactEntity contact = createContact(1L, 1L, 2L, "PENDING");
        when(contactRepository.findById(1L)).thenReturn(Optional.of(contact));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> contactService.acceptRequest(1L, 999L));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void acceptRequest_alreadyAccepted_throwsException() {
        ContactEntity contact = createContact(1L, 1L, 2L, "ACCEPTED");
        when(contactRepository.findById(1L)).thenReturn(Optional.of(contact));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> contactService.acceptRequest(1L, 2L));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void acceptRequest_notFound_throwsResourceNotFoundException() {
        when(contactRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> contactService.acceptRequest(999L, 2L));
    }

    @Test
    void listContacts_returnsAcceptedContacts() {
        ContactEntity contact1 = createContact(1L, 1L, 2L, "ACCEPTED");
        ContactEntity contact2 = createContact(2L, 3L, 1L, "ACCEPTED");
        when(contactRepository.findAcceptedContacts(1L)).thenReturn(List.of(contact1, contact2));

        List<ContactEntity> results = contactService.listContacts(1L);

        assertEquals(2, results.size());
    }

    @Test
    void removeContact_succeeds() {
        ContactEntity contact = createContact(1L, 1L, 2L, "ACCEPTED");
        when(contactRepository.findById(1L)).thenReturn(Optional.of(contact));

        contactService.removeContact(1L, 1L);

        verify(contactRepository).delete(contact);
    }

    @Test
    void getSentRequests_returnsPendingSentRequests() {
        ContactEntity sent = createContact(1L, 1L, 2L, "PENDING");
        when(contactRepository.findByRequesterIdAndStatus(1L, "PENDING")).thenReturn(List.of(sent));

        List<ContactEntity> results = contactService.getSentRequests(1L);

        assertEquals(1, results.size());
        assertEquals("PENDING", results.get(0).getStatus());
        assertEquals(1L, results.get(0).getRequesterId());
    }

    @Test
    void getSentRequests_returnsEmptyWhenNoPendingSent() {
        when(contactRepository.findByRequesterIdAndStatus(1L, "PENDING")).thenReturn(List.of());

        List<ContactEntity> results = contactService.getSentRequests(1L);

        assertTrue(results.isEmpty());
    }

    @Test
    void areContacts_returnsTrueForAcceptedContacts() {
        ContactEntity contact = createContact(1L, 1L, 2L, "ACCEPTED");
        when(contactRepository.findAcceptedContacts(1L)).thenReturn(List.of(contact));

        assertTrue(contactService.areContacts(1L, 2L));
    }

    @Test
    void areContacts_returnsFalseForNonContacts() {
        when(contactRepository.findAcceptedContacts(1L)).thenReturn(List.of());

        assertFalse(contactService.areContacts(1L, 2L));
    }
}
