package com.securechat.contact;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import java.util.List;

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
import com.securechat.security.JwtTokenProvider;
import com.securechat.user.UserEntity;
import com.securechat.user.UserService;

@WebMvcTest(controllers = ContactController.class)
@AutoConfigureMockMvc(addFilters = false)
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ContactService contactService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

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
    void sendRequest_returns201() throws Exception {
        ContactRequestDto request = new ContactRequestDto();
        request.setRecipientId(2L);

        ContactEntity contact = createContact(1L, 1L, 2L, "PENDING");
        when(contactService.sendRequest(1L, 2L)).thenReturn(contact);

        UserEntity otherUser = createUser(2L, "bob");
        when(userService.getUserById(2L)).thenReturn(otherUser);

        mockMvc.perform(post("/api/v1/contacts/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.requesterId").value(1))
            .andExpect(jsonPath("$.recipientId").value(2));
    }

    @Test
    void listContacts_returnsList() throws Exception {
        ContactEntity contact = createContact(1L, 1L, 2L, "ACCEPTED");
        when(contactService.listContacts(1L)).thenReturn(List.of(contact));

        UserEntity otherUser = createUser(2L, "bob");
        when(userService.getUserById(2L)).thenReturn(otherUser);

        mockMvc.perform(get("/api/v1/contacts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("ACCEPTED"));
    }

    @Test
    void pendingRequests_returnsPending() throws Exception {
        ContactEntity contact = createContact(1L, 3L, 1L, "PENDING");
        when(contactService.getPendingRequests(1L)).thenReturn(List.of(contact));

        UserEntity otherUser = createUser(3L, "charlie");
        when(userService.getUserById(3L)).thenReturn(otherUser);

        mockMvc.perform(get("/api/v1/contacts/pending"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void sentRequests_returnsSentPending() throws Exception {
        ContactEntity contact = createContact(1L, 1L, 3L, "PENDING");
        when(contactService.getSentRequests(1L)).thenReturn(List.of(contact));

        UserEntity otherUser = createUser(3L, "charlie");
        when(userService.getUserById(3L)).thenReturn(otherUser);

        mockMvc.perform(get("/api/v1/contacts/sent"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("PENDING"))
            .andExpect(jsonPath("$[0].requesterId").value(1));
    }

    @Test
    void acceptRequest_returns200() throws Exception {
        ContactEntity contact = createContact(1L, 1L, 2L, "ACCEPTED");
        when(contactService.acceptRequest(1L, 1L)).thenReturn(contact);

        UserEntity otherUser = createUser(2L, "bob");
        when(userService.getUserById(2L)).thenReturn(otherUser);

        mockMvc.perform(post("/api/v1/contacts/1/accept"))
            .andExpect(status().isOk());
    }

    @Test
    void removeContact_returns204() throws Exception {
        doNothing().when(contactService).removeContact(1L, 1L);

        mockMvc.perform(delete("/api/v1/contacts/1"))
            .andExpect(status().isNoContent());
    }

    private ContactEntity createContact(Long id, Long requesterId, Long recipientId, String status) {
        ContactEntity contact = new ContactEntity();
        contact.setId(id);
        contact.setRequesterId(requesterId);
        contact.setRecipientId(recipientId);
        contact.setStatus(status);
        return contact;
    }

    private UserEntity createUser(Long id, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setDisplayName(username);
        user.setEmail(username + "@example.com");
        return user;
    }
}
