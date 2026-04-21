package com.example.instagramclone.infrastructure.websocket;

import java.security.Principal;

/**
 * STOMP 세션에 바인딩되는 인증 정보.
 *
 * java.security.Principal 인터페이스의 getName() 반환 타입이 String 이라
 * Long memberId 를 문자열로 변환해 노출한다. 사용하는 쪽에서 Long.parseLong() 으로 복구.
 */
public record StompPrincipal(Long memberId) implements Principal {

    @Override
    public String getName() {
        return String.valueOf(memberId);
    }
}
