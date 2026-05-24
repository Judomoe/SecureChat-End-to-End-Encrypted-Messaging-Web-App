package com.securechat.contact;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.securechat.exception.ResourceNotFoundException;
import com.securechat.user.UserEntity;
import com.securechat.user.UserRepository;

@Service
public class ContactService {

    private final ContactRepository contactRepository;
    private final UserRepository userRepository;

    public ContactService(ContactRepository contactRepository, UserRepository userRepository) {
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
    }

    public ContactEntity sendRequest(Long requesterId, Long recipientId) {
        if (requesterId.equals(recipientId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot send contact request to yourself");
        }

        userRepository.findById(recipientId)
            .orElseThrow(() -> new ResourceNotFoundException("Recipient not found"));

        if (contactRepository.existsByRequesterIdAndRecipientId(requesterId, recipientId) ||
            contactRepository.existsByRequesterIdAndRecipientId(recipientId, requesterId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Contact request already exists");
        }

        ContactEntity contact = new ContactEntity();
        contact.setRequesterId(requesterId);
        contact.setRecipientId(recipientId);
        contact.setStatus("PENDING");

        return contactRepository.save(contact);
    }

    public ContactEntity acceptRequest(Long contactId, Long userId) {
        ContactEntity contact = contactRepository.findById(contactId)
            .orElseThrow(() -> new ResourceNotFoundException("Contact request not found"));

        if (!contact.getRecipientId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to accept this request");
        }

        if (!"PENDING".equals(contact.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contact request is not pending");
        }

        contact.setStatus("ACCEPTED");
        return contactRepository.save(contact);
    }

    public List<ContactEntity> listContacts(Long userId) {
        return contactRepository.findAcceptedContacts(userId);
    }

    public List<ContactEntity> getSentRequests(Long userId) {
        return contactRepository.findByRequesterIdAndStatus(userId, "PENDING");
    }

    public List<ContactEntity> getPendingRequests(Long userId) {
        return contactRepository.findByRecipientIdAndStatus(userId, "PENDING");
    }

    public void removeContact(Long contactId, Long userId) {
        ContactEntity contact = contactRepository.findById(contactId)
            .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

        if (!contact.getRequesterId().equals(userId) && !contact.getRecipientId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to remove this contact");
        }

        contactRepository.delete(contact);
    }

    public boolean areContacts(Long userId1, Long userId2) {
        List<ContactEntity> accepted = contactRepository.findAcceptedContacts(userId1);
        return accepted.stream().anyMatch(c ->
            (c.getRequesterId().equals(userId1) && c.getRecipientId().equals(userId2)) ||
            (c.getRequesterId().equals(userId2) && c.getRecipientId().equals(userId1))
        );
    }
}
