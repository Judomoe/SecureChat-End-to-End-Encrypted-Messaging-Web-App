package com.securechat.keyexchange;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;

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
import com.securechat.conversation.ConversationService;
import com.securechat.security.JwtTokenProvider;

@WebMvcTest(controllers = KeyExchangeController.class)
@AutoConfigureMockMvc(addFilters = false)
class KeyExchangeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private KeyExchangeService keyExchangeService;

    @MockitoBean
    private ConversationService conversationService;

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
    void exchangeKey_returns201() throws Exception {
        KeyExchangeRequest request = new KeyExchangeRequest();
        request.setRecipientId(2L);
        request.setEncryptedAesKey("encrypted-aes-key");
        request.setSignature("signature-base64");

        KeyMaterialEntity entity = new KeyMaterialEntity();
        entity.setId(1L);
        entity.setConversationId(10L);
        entity.setRecipientId(2L);
        entity.setSenderId(1L);
        entity.setEncryptedAesKey("encrypted-aes-key");
        entity.setSignature("signature-base64");
        when(keyExchangeService.storeKeyMaterial(eq(1L), eq(10L), eq(2L), eq("encrypted-aes-key"), eq("signature-base64")))
            .thenReturn(entity);

        mockMvc.perform(post("/api/v1/conversations/10/keys/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.senderId").value(1));
    }

    @Test
    void getPendingKey_returnsKeyMaterial() throws Exception {
        KeyMaterialEntity entity = new KeyMaterialEntity();
        entity.setId(1L);
        entity.setConversationId(10L);
        entity.setRecipientId(1L);
        entity.setSenderId(2L);
        entity.setEncryptedAesKey("encrypted-aes-key");
        entity.setSignature("signature-base64");
        when(keyExchangeService.fetchPendingKey(10L, 1L)).thenReturn(entity);

        mockMvc.perform(get("/api/v1/conversations/10/keys/pending"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.encryptedAesKey").value("encrypted-aes-key"));
    }

    @Test
    void confirmKey_returns200() throws Exception {
        doNothing().when(keyExchangeService).confirmKey(10L, 1L);

        mockMvc.perform(post("/api/v1/conversations/10/keys/confirm"))
            .andExpect(status().isOk());
    }
}
