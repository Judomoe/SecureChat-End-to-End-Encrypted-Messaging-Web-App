package com.securechat.conversation;

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

import com.securechat.contact.ContactService;
import com.securechat.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationMemberRepository conversationMemberRepository;

    @Mock
    private ContactService contactService;

    @InjectMocks
    private ConversationService conversationService;

    @Test
    void createConversation_succeeds() {
        when(contactService.areContacts(1L, 2L)).thenReturn(true);
        when(conversationMemberRepository.findCommonConversationIds(1L, 2L)).thenReturn(List.of());
        when(conversationRepository.save(any(ConversationEntity.class))).thenAnswer(invocation -> {
            ConversationEntity c = invocation.getArgument(0);
            c.setId(10L);
            return c;
        });
        when(conversationMemberRepository.save(any(ConversationMemberEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConversationEntity result = conversationService.createConversation(1L, 2L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        verify(conversationMemberRepository, times(2)).save(any(ConversationMemberEntity.class));
    }

    @Test
    void createConversation_returnsExistingIfDuplicate() {
        ConversationEntity existing = new ConversationEntity();
        existing.setId(5L);

        when(contactService.areContacts(1L, 2L)).thenReturn(true);
        when(conversationMemberRepository.findCommonConversationIds(1L, 2L)).thenReturn(List.of(5L));
        when(conversationRepository.findById(5L)).thenReturn(java.util.Optional.of(existing));

        ConversationEntity result = conversationService.createConversation(1L, 2L);

        assertEquals(5L, result.getId());
        verify(conversationRepository, never()).save(any(ConversationEntity.class));
        verify(conversationMemberRepository, never()).save(any(ConversationMemberEntity.class));
    }

    @Test
    void createConversation_withNonContact_throwsException() {
        when(contactService.areContacts(1L, 2L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> conversationService.createConversation(1L, 2L));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void listConversations_returnsUserConversations() {
        ConversationEntity conv = new ConversationEntity();
        conv.setId(1L);
        when(conversationMemberRepository.findConversationIdsByUserId(1L)).thenReturn(List.of(1L));
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conv));

        List<ConversationEntity> results = conversationService.listConversations(1L);

        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getId());
    }

    @Test
    void getConversation_succeedsForMember() {
        ConversationEntity conv = new ConversationEntity();
        conv.setId(1L);
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conv));
        when(conversationMemberRepository.existsByConversationIdAndUserId(1L, 1L)).thenReturn(true);

        ConversationEntity result = conversationService.getConversation(1L, 1L);

        assertEquals(1L, result.getId());
    }

    @Test
    void getConversation_throwsExceptionForNonMember() {
        ConversationEntity conv = new ConversationEntity();
        conv.setId(1L);
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conv));
        when(conversationMemberRepository.existsByConversationIdAndUserId(1L, 999L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> conversationService.getConversation(1L, 999L));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void getConversation_notFound_throwsResourceNotFoundException() {
        when(conversationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> conversationService.getConversation(999L, 1L));
    }

    @Test
    void isMember_returnsCorrectResult() {
        when(conversationMemberRepository.existsByConversationIdAndUserId(1L, 1L)).thenReturn(true);
        when(conversationMemberRepository.existsByConversationIdAndUserId(1L, 999L)).thenReturn(false);

        assertTrue(conversationService.isMember(1L, 1L));
        assertFalse(conversationService.isMember(1L, 999L));
    }
}
