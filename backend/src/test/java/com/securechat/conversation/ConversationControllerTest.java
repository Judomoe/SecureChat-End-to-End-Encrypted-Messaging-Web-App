package com.securechat.conversation;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securechat.message.MessageEntity;
import com.securechat.message.MessageRepository;
import com.securechat.security.JwtTokenProvider;
import com.securechat.user.UserEntity;
import com.securechat.user.UserRepository;

@WebMvcTest(controllers = ConversationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ConversationService conversationService;

    @MockitoBean
    private ConversationMemberRepository conversationMemberRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private MessageRepository messageRepository;

    @BeforeEach
    void setUp() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createConversation_returns201() throws Exception {
        CreateConversationRequest request = new CreateConversationRequest();
        request.setRecipientId(2L);

        ConversationEntity conv = new ConversationEntity();
        conv.setId(1L);
        when(conversationService.createConversation(1L, 2L)).thenReturn(conv);

        ConversationMemberEntity member1 = createMember(1L, 1L);
        ConversationMemberEntity member2 = createMember(1L, 2L);
        when(conversationService.getMembers(1L)).thenReturn(List.of(member1, member2));

        UserEntity user1 = createUser(1L, "alice");
        UserEntity user2 = createUser(2L, "bob");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(messageRepository.findLastByConversationId(eq(1L), any())).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void listConversations_returnsList() throws Exception {
        ConversationEntity conv = new ConversationEntity();
        conv.setId(1L);
        when(conversationService.listConversations(1L)).thenReturn(List.of(conv));

        ConversationMemberEntity member = createMember(1L, 1L);
        when(conversationService.getMembers(1L)).thenReturn(List.of(member));

        UserEntity user = createUser(1L, "alice");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(messageRepository.findLastByConversationId(eq(1L), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/conversations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getConversation_returnsConversation() throws Exception {
        ConversationEntity conv = new ConversationEntity();
        conv.setId(1L);
        when(conversationService.getConversation(1L, 1L)).thenReturn(conv);

        ConversationMemberEntity member = createMember(1L, 1L);
        when(conversationService.getMembers(1L)).thenReturn(List.of(member));

        UserEntity user = createUser(1L, "alice");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(messageRepository.findLastByConversationId(eq(1L), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/conversations/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }

    private ConversationMemberEntity createMember(Long conversationId, Long userId) {
        ConversationMemberEntity member = new ConversationMemberEntity();
        member.setId(1L);
        member.setConversationId(conversationId);
        member.setUserId(userId);
        return member;
    }

    private UserEntity createUser(Long id, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        return user;
    }
}
