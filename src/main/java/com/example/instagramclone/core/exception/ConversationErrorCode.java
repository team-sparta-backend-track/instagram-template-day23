package com.example.instagramclone.core.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ConversationErrorCode implements ErrorCode {
    CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND, "CV001", "대화방을 찾을 수 없습니다."),
    SELF_CONVERSATION(HttpStatus.BAD_REQUEST, "CV002", "자기 자신과는 대화할 수 없습니다."),
    CONVERSATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CV003", "대화방에 접근할 권한이 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
