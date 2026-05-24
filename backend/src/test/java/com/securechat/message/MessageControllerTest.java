package com.securechat.message;

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

@WebMvcTest(controllers = MessageController.class)
@AutoConfigureMockMvc(addFilters = false)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MessageService messageService;

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
    void sendMessage_returns201() throws Exception {
        SendMessageRequest request = new SendMessageRequest();
        request.setCiphertext("ciphertext-data");
        request.setIv("iv-data");
        request.setHmac("hmac-data");
        request.setSignature("signature-data");
        request.setTimestamp(System.currentTimeMillis());

        MessageEntity entity = new MessageEntity();
        entity.setId(1L);
        entity.setConversationId(10L);
        entity.setSenderId(1L);
        entity.setCiphertext("ciphertext-data");
        entity.setIv("iv-data");
        entity.setHmac("hmac-data");
        entity.setSignature("signature-data");
        entity.setTimestamp(request.getTimestamp());

        when(messageService.sendMessage(eq(1L), eq(10L), any(SendMessageRequest.class))).thenReturn(entity);

        mockMvc.perform(post("/api/v1/conversations/10/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.ciphertext").value("ciphertext-data"));
    }

    @Test
    void getMessages_returnsMessages() throws Exception {
        MessageEntity msg1 = new MessageEntity();
        msg1.setId(1L);
        msg1.setConversationId(10L);
        msg1.setSenderId(2L);
        msg1.setCiphertext("cipher1");
        msg1.setIv("iv1");
        msg1.setHmac("hmac1");
        msg1.setSignature("sig1");
        msg1.setTimestamp(System.currentTimeMillis());

        MessageEntity msg2 = new MessageEntity();
        msg2.setId(2L);
        msg2.setConversationId(10L);
        msg2.setSenderId(1L);
        msg2.setCiphertext("cipher2");
        msg2.setIv("iv2");
        msg2.setHmac("hmac2");
        msg2.setSignature("sig2");
        msg2.setTimestamp(System.currentTimeMillis());

        when(messageService.getMessages(10L, 1L, 50, null)).thenReturn(List.of(msg2, msg1));

        mockMvc.perform(get("/api/v1/conversations/10/messages"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }
}
