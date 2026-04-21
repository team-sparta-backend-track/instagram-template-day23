package com.example.instagramclone.infrastructure.security;

/**
 * 인증 없이 접근 가능한 경로 패턴 모음.
 *
 * SecurityConfig 의 permitAll 리스트와 JwtAuthenticationFilter.shouldNotFilter 가
 * 동일한 기준으로 동작하도록 한 곳에서 관리한다. 여기를 고치지 않은 채
 * 한쪽만 고치면 "인가는 통과하는데 필터는 타는" 어정쩡한 상태가 되므로 주의.
 */
public final class PublicPathPatterns {

    /** HTTP 메서드 무관하게 인증 없이 허용하는 경로 */
    public static final String[] ANY_METHOD = {
            "/",
            "/assets/**", "/img/**", "/error", "/favicon.ico",
            "/h2-console/**",
            "/actuator/**",
            "/api/benchmark/offset-vs-cursor",
            "/.well-known/**",            // Chrome DevTools 등 브라우저 자동 프로브
            "/ws-test.html",              // Day 22: STOMP 수동 테스트 페이지
            "/ws/**"                      // Day 22: WebSocket 핸드셰이크
    };

    /** POST 만 인증 없이 허용하는 경로 (회원가입/로그인 등) */
    public static final String[] PUBLIC_POST = {
            "/api/auth/login",
            "/api/auth/signup",
            "/api/auth/reissue"
    };

    /** GET 만 인증 없이 허용하는 경로 */
    public static final String[] PUBLIC_GET = {
            "/api/auth/check-duplicate"
    };

    private PublicPathPatterns() {
    }
}
