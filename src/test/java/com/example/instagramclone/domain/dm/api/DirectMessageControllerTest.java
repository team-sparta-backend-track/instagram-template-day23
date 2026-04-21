package com.example.instagramclone.domain.dm.api;

import com.example.instagramclone.core.common.dto.ApiResponse;
import com.example.instagramclone.core.common.dto.SliceResponse;
import com.example.instagramclone.domain.dm.application.DirectMessageService;
import com.example.instagramclone.domain.dm.dto.DirectMessageResponse;
import com.example.instagramclone.infrastructure.security.dto.LoginUserInfoDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verify;

/**
 * DirectMessageController 단위 테스트.
 *
 * [테스트 범위]
 * - @LoginUser 로 추출한 memberId 가 서비스로 그대로 넘어간다
 * - size 상한/하한이 안전하게 클램프된다 (1..50)
 * - SliceResponse<DirectMessageResponse> 가 ApiResponse 본문으로 래핑된다
 */
@ExtendWith(MockitoExtension.class)
class DirectMessageControllerTest {

    @Mock
    private DirectMessageService directMessageService;

    @InjectMocks
    private DirectMessageController controller;

    private LoginUserInfoDto loginUser(Long id) {
        return new LoginUserInfoDto(id);
    }

    @Test
    @DisplayName("cursor null + 기본 size 20 을 그대로 서비스에 전달, 200 OK 본문에 SliceResponse 포함")
    void passes_through_cursor_and_default_size() {
        SliceResponse<DirectMessageResponse> slice = SliceResponse.of(false, List.of(
                new DirectMessageResponse(1L, 10L, 2L, "kuromi", "hi", LocalDateTime.now())
        ));

        given(directMessageService.getMessages(10L, 7L, null, 20)).willReturn(slice);

        ResponseEntity<ApiResponse<SliceResponse<DirectMessageResponse>>> response =
                controller.getMessages(10L, null, 20, loginUser(7L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(slice);
    }

    @Test
    @DisplayName("size 가 50 을 초과하면 50으로 클램프되어 서비스로 전달된다")
    void clamps_size_upper_bound() {
        given(directMessageService.getMessages(10L, 7L, 5L, 50))
                .willReturn(SliceResponse.of(false, List.of()));

        controller.getMessages(10L, 5L, 9999, loginUser(7L));

        verify(directMessageService).getMessages(10L, 7L, 5L, 50);
    }

    @Test
    @DisplayName("size 가 0 이하이면 1로 클램프되어 서비스로 전달된다")
    void clamps_size_lower_bound() {
        given(directMessageService.getMessages(10L, 7L, null, 1))
                .willReturn(SliceResponse.of(false, List.of()));

        controller.getMessages(10L, null, 0, loginUser(7L));

        then(directMessageService).should().getMessages(10L, 7L, null, 1);
    }

    @Test
    @DisplayName("markAsRead() - 서비스가 반환한 변경 행수를 200 OK 본문으로 래핑한다")
    void mark_as_read_returns_updated_count() {
        given(directMessageService.markMessagesAsRead(10L, 7L)).willReturn(5);

        ResponseEntity<ApiResponse<Integer>> response =
                controller.markAsRead(10L, loginUser(7L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(5);
    }
}
