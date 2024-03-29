package com.giftforyoube.scheduler;

import com.giftforyoube.funding.entity.Funding;
import com.giftforyoube.funding.entity.FundingStatus;
import com.giftforyoube.funding.repository.FundingRepository;
import com.giftforyoube.funding.service.CacheService;
import com.giftforyoube.notification.entity.NotificationType;
import com.giftforyoube.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j(topic = "Scheduler")
@Component
@RequiredArgsConstructor
public class Scheduler {

    private final FundingRepository fundingRepository;
    private final NotificationService notificationService;
    private final CacheService cacheService;
    // 매일 자정에 실행, 마감일이 지난 펀딩의 상태를 업데이트
    // 초, 분, 시, 일, 월, 주 순서
    @Scheduled(cron = "0 0 0 * * ?")
    @CacheEvict(value = {"activeMainFundings", "activeFundings", "finishedFundings", "fundingDetail"}, allEntries = true)
    public void autoFinishFundings() {
        log.info("마감일 종료 상태 업데이트 실행");
        LocalDate currentDate = LocalDate.now();
//        List<Funding> fundings = fundingRepository.findByEndDateLessThanAndStatus(currentDate, FundingStatus.ACTIVE);
        List<Funding> fundings = fundingRepository.findByEndDateLessThanEqualAndStatus(currentDate, FundingStatus.ACTIVE);
        for (Funding funding : fundings) {
            funding.setStatus(FundingStatus.FINISHED);
            fundingRepository.save(funding);


            // 이메일 수신 동의 했을때
            if (funding.getUser().getIsEmailNotificationAgreed()) {
                // 알림메세지 발송
                log.info("[autoFinishFundings] 펀딩 마감! 펀딩 종료!");
                String content = "펀딩 마감일이되어 펀딩이 종료되었습니다.";
                String url = "https://giftipie.me/fundingdetail/" + funding.getId();
                NotificationType notificationType = NotificationType.FUNDING_TIME_OUT;
                notificationService.send(funding.getUser(), notificationType, content, url);
            }

        }
        cacheService.clearFundingCaches();
    }
}