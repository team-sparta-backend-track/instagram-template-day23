package com.example.instagramclone.infrastructure.websocket;

import com.example.instagramclone.infrastructure.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * StompChannelInterceptor 단위 테스트.
 *
 * [테스트 범위]
 * - CONNECT 프레임에서만 JWT 검증 수행
 * - 토큰이 없거나 유효하지 않으면 MessageDeliveryException 발생
 * - 유효한 토큰이면 StompPrincipal 을 세션에 바인딩
 * - CONNECT 외 프레임(SEND, SUBSCRIBE 등)은 검증 없이 통과
 */
@ExtendWith(MockitoExtension.class)
class StompChannelInterceptorTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private StompChannelInterceptor interceptor;

    private final MessageChannel channel = mock(MessageChannel.class);

    private Message<byte[]> buildStompMessage(StompCommand command, String authHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        if (authHeader != null) {
            accessor.addNativeHeader("Authorization", authHeader);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Nested
    @DisplayName("CONNECT 프레임")
    class OnConnect {

        @Test
        @DisplayName("실패 - Authorization 헤더가 없으면 MessageDeliveryException")
        void fail_when_header_missing() {
            Message<byte[]> message = buildStompMessage(StompCommand.CONNECT, null);

            assertThatThrownBy(() -> interceptor.preSend(message, channel))
                    .isInstanceOf(MessageDeliveryException.class);

            then(jwtTokenProvider).should(never()).getMemberId(anyString());
        }

        @Test
        @DisplayName("실패 - Bearer 접두어가 아닌 헤더면 MessageDeliveryException")
        void fail_when_header_not_bearer() {
            Message<byte[]> message = buildStompMessage(StompCommand.CONNECT, "Basic abc.def");

            assertThatThrownBy(() -> interceptor.preSend(message, channel))
                    .isInstanceOf(MessageDeliveryException.class);
        }

        @Test
        @DisplayName("실패 - 토큰 검증에 실패하면 MessageDeliveryException")
        void fail_when_token_invalid() {
            given(jwtTokenProvider.validateToken("badToken")).willReturn(false);
            Message<byte[]> message =
                    buildStompMessage(StompCommand.CONNECT, "Bearer badToken");

            assertThatThrownBy(() -> interceptor.preSend(message, channel))
                    .isInstanceOf(MessageDeliveryException.class);

            then(jwtTokenProvider).should(never()).getMemberId(anyString());
        }

        @Test
        @DisplayName("성공 - 유효한 토큰이면 StompPrincipal 을 세션에 바인딩한다")
        void success_sets_principal_with_member_id() {
            given(jwtTokenProvider.validateToken("validToken")).willReturn(true);
            given(jwtTokenProvider.getMemberId("validToken")).willReturn(42L);

            Message<byte[]> message =
                    buildStompMessage(StompCommand.CONNECT, "Bearer validToken");

            Message<?> result = interceptor.preSend(message, channel);

            StompHeaderAccessor accessor = MessageHeaderAccessor
                    .getAccessor(result, StompHeaderAccessor.class);
            assertThat(accessor).isNotNull();
            assertThat(accessor.getUser()).isInstanceOf(StompPrincipal.class);

            StompPrincipal principal = (StompPrincipal) accessor.getUser();
            assertThat(principal.memberId()).isEqualTo(42L);
            assertThat(principal.getName()).isEqualTo("42");
        }
    }

    @Nested
    @DisplayName("CONNECT 외 프레임")
    class OnOtherFrames {

        @Test
        @DisplayName("SEND 프레임은 JWT 검증을 수행하지 않는다")
        void send_frame_skips_jwt_validation() {
            Message<byte[]> message = buildStompMessage(StompCommand.SEND, null);

            Message<?> result = interceptor.preSend(message, channel);

            assertThat(result).isSameAs(message);
            then(jwtTokenProvider).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("SUBSCRIBE 프레임은 JWT 검증을 수행하지 않는다")
        void subscribe_frame_skips_jwt_validation() {
            Message<byte[]> message = buildStompMessage(StompCommand.SUBSCRIBE, null);

            Message<?> result = interceptor.preSend(message, channel);

            assertThat(result).isSameAs(message);
            then(jwtTokenProvider).shouldHaveNoInteractions();
        }
    }

    // helper for then().should(never()).getMemberId(anyString()) — import shortcut
    private static String anyString() {
        return org.mockito.ArgumentMatchers.anyString();
    }
}
