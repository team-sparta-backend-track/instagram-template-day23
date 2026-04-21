package com.example.instagramclone.domain.notification.domain;

public enum NotificationType {
    LIKE,       // 좋아요
    FOLLOW,     // 팔로우
    COMMENT,    // 댓글
    MENTION,    // 멘션
    DM          // DM — targetId = conversationId (탭 시 대화방 이동)
}
