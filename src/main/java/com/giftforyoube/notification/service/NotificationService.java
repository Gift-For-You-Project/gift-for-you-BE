package com.giftforyoube.notification.service;

import com.giftforyoube.global.exception.BaseException;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.notification.dto.NotificationResponseDto;
import com.giftforyoube.notification.dto.SubscribeDummyDto;
import com.giftforyoube.notification.entity.Notification;
import com.giftforyoube.notification.entity.NotificationType;
import com.giftforyoube.notification.repository.EmitterRepository;
import com.giftforyoube.notification.repository.NotificationRepository;
import com.giftforyoube.user.entity.User;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60; // SSE 연결 지속 시간 (1시간)
    private final EmitterRepository emitterRepository;
    private final NotificationRepository notificationRepository;
    private final MailingService mailingService;

    // subscribe
    public SseEmitter sseSubscribe(String username, String lastEventId, HttpServletResponse response) {
        log.info("sse 연결 시작...");
        String emitterId = createTimeIncludeId(username);

        // 클라이언트의 SSE 연결 요청에 응답하기 위한 SseEmitter 객체 생성
        // 유효시간 지정으로 시간이 지나면 클라이언트에서 자동으로 재연결 요청
        SseEmitter emitter = emitterRepository.save(emitterId, new SseEmitter(DEFAULT_TIMEOUT));

        // Nginx Proxy에서의 필요설정. 불필요한 버퍼링 방지
        response.setHeader("X-Accel-Buffering", "no");

        // SseEmitter 의 완료/시간초과/에러로 인한 전송 불가 시 sseEmitter 삭제
        emitter.onCompletion(() -> emitterRepository.deleteAllEmitterStartWithId(emitterId));
        emitter.onTimeout(() -> emitterRepository.deleteAllEmitterStartWithId(emitterId));
        emitter.onError((e) -> emitterRepository.deleteAllEmitterStartWithId(emitterId));

        String eventId = createTimeIncludeId(username);
        // 수 많은 이벤트 들을 구분하기 위해 이벤트 ID에 시간을 통해 구분을 해줌
        sendNotification(emitter, eventId, emitterId, new SubscribeDummyDto(username));

        // 클라이언트가 미수신한 Event 목록이 존재할 경우 전송하여 Event 유실을 예방
        if (hasLostData(lastEventId)) {
            sendLostData(lastEventId, username, emitterId, emitter);
        }
        log.info("sse 연결 완료");
        return emitter;
    }


    // 알람 send
    @Async
    public void send(User receiver, NotificationType notificationType, String content, String url) {
        log.info("메세지 send 시작....");
        // notification 객체 생성 후 db 저장
        Notification notification = createNotification(receiver, notificationType, content, url);
        Notification saveNotification = notificationRepository.save(notification);

        String receiverId = receiver.getNickname();
        String eventId = receiverId + "_" + System.currentTimeMillis();

        // 특정 사용자의 모든 SseEmitter 호출하여 emitters 생성
        Map<String, SseEmitter> emitters = emitterRepository.findAllEmitterStartWithByUserId(receiverId);

        // 호출된 emitter들을 EventCache에 각각 저장 후 각 emitter를 통해 sendNotification으로 알림을 보냄
        // EventCache에 저장 -> 연결이 끊긴 후 다시 연결될 때 놓친 알림을 클라이언트에게 재 전송을 위함
        emitters.forEach(
                (emitterId, emitter) -> {
                    emitterRepository.saveEventCache(emitterId, saveNotification);

                    sendNotification(emitter, eventId, emitterId, new NotificationResponseDto(saveNotification.getId(),
                            saveNotification.getContent(), saveNotification.getUrl(),
                            saveNotification.getNotificationType(), saveNotification.getIsRead(), saveNotification.getCreatedAt()));
                }
        );

        // 이메일 수신 동의 했을때
        if (saveNotification.getReceiver().getIsEmailNotificationAgreed()) {
            log.info("sse 메시지 발송 완료. 알림 이메일 발송 시작");
            // 이메일 알림 발송
            try {
                mailingService.sendNotificationEmail(saveNotification);
            } catch (MessagingException e) {
                throw new BaseException(BaseResponseStatus.EMAIL_SEND_FAILED);
            }
        }
    }

    // 알림 객체 생성
    private Notification createNotification(User receiver, NotificationType notificationType, String content, String url) {
        return Notification.builder()
                .receiver(receiver)
                .notificationType(notificationType)
                .content(content)
                .url(url)
                .isRead(false)
                .build();
    }

    // 누락된 데이터 전송
    private void sendLostData(String lastEventId, String username, String emitterId, SseEmitter emitter) {
        Map<String, Object> eventCaches = emitterRepository.findAllEventCacheStartWithByUserId(username); // 이벤트 캐시 조회
        eventCaches.entrySet().stream()
                .filter(entry -> lastEventId.compareTo(entry.getKey()) < 0) // 놓친 이벤트 필터링
                .forEach(entry -> sendNotification(emitter, entry.getKey(), emitterId, entry.getValue())); // 놓친 이벤트 전송
    }

    // 누락된 데이터 확인
    private boolean hasLostData(String lastEventId) {
        return !lastEventId.isEmpty();
    }

    // 알림 발송
    private void sendNotification(SseEmitter emitter, String eventId, String emitterId, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(eventId)
                    .name("sse")
                    .data(data)
            );
        } catch (IOException exception) {
            emitterRepository.deleteById(emitterId);
            throw new BaseException(BaseResponseStatus.NOTIFICATION_SEND_FAILED);
        }
    }

    private String createTimeIncludeId(String username) {
        return username + "_" + System.currentTimeMillis();
    }

    // user로 해당 user의 전체 알림목록을 조회
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getNotifications(User user) {
        List<Notification> notificationList = notificationRepository.findAllByReceiverOrderByCreatedAtDesc(user);
        return notificationList.stream().map(NotificationResponseDto::new).toList();
    }

    // 알림 읽음처리
    @Transactional
    public NotificationResponseDto readNotification(User user, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElseThrow(
                () -> new BaseException(BaseResponseStatus.NOTIFICATION_NOT_FOUND));

        if (!notification.getReceiver().getId().equals(user.getId())) {
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED_READ_NOTIFICATION);
        }

        notification.setIsRead(true);
        Notification saveNotification = notificationRepository.save(notification);
        return new NotificationResponseDto(saveNotification);
    }

    // 해당 유저 읽은 알림 메세지 전체 삭제
    @Transactional
    public void deleteNotificationIsReadTrue(User user) {
        List<Notification> notificationList = notificationRepository.findAllByReceiverAndIsReadTrue(user);
        log.info(notificationList.toString());

        if (notificationList.isEmpty()) {
            throw new BaseException(BaseResponseStatus.READ_NOTIFICATION_LIST_NOT_FOUND);
        }

        notificationRepository.deleteAll(notificationList);
    }

    // 해당 유저 원하는 알림 메세지 삭제
    @Transactional
    public void deleteNotification(User user, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElseThrow(
                () -> new BaseException(BaseResponseStatus.NOTIFICATION_NOT_FOUND));

        if (!notification.getReceiver().getId().equals(user.getId())) {
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED_DELETE_NOTIFICATION);
        }

        notificationRepository.delete(notification);
    }
}
