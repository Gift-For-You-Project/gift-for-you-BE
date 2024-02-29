package com.giftforyoube.notification.controller;

import com.giftforyoube.global.exception.BaseException;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.security.UserDetailsImpl;
import com.giftforyoube.notification.dto.NotificationResponseDto;
import com.giftforyoube.notification.entity.NotificationType;
import com.giftforyoube.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "알림", description = "알림 관련 API")
public class NotificationController {
    private final NotificationService notificationService;


    // 알림메세지 테스트
    @PostMapping("/test")
    public ResponseEntity<String> NotificationTest(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        String testContent = "테스트 알림입니다!";
        String testUrl = "https://the2.sfo2.cdn.digitaloceanspaces.com/m_photo/411589.webp";
        NotificationType testNotificationType = NotificationType.DONATION;
        notificationService.send(userDetails.getUser(), testNotificationType, testContent, testUrl);

        String responseText = "알림메시지 발송 성공";
        return new ResponseEntity<>(responseText, HttpStatus.OK);
    }

    /////

    // 사용자 SSE 연결 API
    @GetMapping(value = "/subscribe", produces = "text/event-stream")
    public ResponseEntity<SseEmitter> sseConnect(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                 @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") String lastEventId,
                                                 HttpServletResponse response) {
        return new ResponseEntity<>(notificationService.sseSubscribe(userDetails.getUsername(),lastEventId, response), HttpStatus.OK);
    }

    // 전체 알림 조회 API
    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>> getNotifications(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED_GET_NOTIFICATION);
        }
        return new ResponseEntity<>(notificationService.getNotifications(userDetails.getUser()), HttpStatus.OK);
    }

    // 해당 알림 조회 시 읽음 처리 API
    @PatchMapping("/{notificationId}")
    public ResponseEntity<NotificationResponseDto> readNotification(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                 @PathVariable Long notificationId) {
        return new ResponseEntity<>(notificationService.readNotification(userDetails.getUser(), notificationId), HttpStatus.OK);
    }

    // 해당 유저 읽은 알림 메세지 전체 삭제
    @DeleteMapping
    public ResponseEntity<?> deleteNotificationIsReadTrue(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        notificationService.deleteNotificationIsReadTrue(userDetails.getUser());
        return ResponseEntity.ok().body("읽은 모든 알림 메세지를 성공적으로 삭제하였습니다.");
    }

    // 해당 유저 원하는 알림 메세지 삭제
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<?> deleteNotification(@AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable Long notificationId) {
        notificationService.deleteNotification(userDetails.getUser(), notificationId);
        return ResponseEntity.ok().body("해당 알림 메세지를 성공적으로 삭제하였습니다.");
    }
}
