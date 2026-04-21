package com.example.instagramclone.core.exception;

public class ConversationException extends BusinessException {

    public ConversationException(ErrorCode errorCode) {
        super(errorCode);
    }
}
