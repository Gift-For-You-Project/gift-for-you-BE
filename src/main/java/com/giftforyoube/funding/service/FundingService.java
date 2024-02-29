package com.giftforyoube.funding.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.giftforyoube.donation.repository.DonationRepository;
import com.giftforyoube.funding.dto.*;
import com.giftforyoube.funding.entity.Funding;
import com.giftforyoube.funding.entity.FundingItem;
import com.giftforyoube.funding.entity.FundingStatus;
import com.giftforyoube.funding.entity.FundingSummary;
import com.giftforyoube.funding.repository.FundingRepository;
import com.giftforyoube.funding.repository.FundingSummaryRepository;
import com.giftforyoube.global.exception.BaseException;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.user.entity.User;
import com.giftforyoube.user.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static org.hibernate.query.sqm.tree.SqmNode.log;

@Service
@Getter
@RequiredArgsConstructor
public class FundingService {

    private final FundingRepository fundingRepository;
    private final UserRepository userRepository;
    private final DonationRepository donationRepository;
    private final FundingSummaryRepository fundingSummaryRepository;

    public FundingItemResponseDto addLinkAndSaveToCache(AddLinkRequestDto requestDto, Long userId) throws IOException {
        log.info("[addLinkAndSaveToCache] 상품링크 처리");

        FundingItem fundingItem = previewItem(requestDto.getItemLink());
        return FundingItemResponseDto.fromEntity(fundingItem);
    }

    @Transactional
    public FundingResponseDto saveToDatabase(FundingCreateRequestDto requestDto, Long userId) throws IOException {
        log.info("[saveToDatabase] DB에 저장하기");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("해당 회원을 찾을 수 없습니다."));
        boolean hasActiveFunding = user.getFundings().stream()
                .anyMatch(funding -> funding.getStatus() == FundingStatus.ACTIVE);
        if (hasActiveFunding) {
            throw new IllegalStateException("이미 진행중인 펀딩이 있습니다.");
        }

        FundingItem fundingItem = previewItem("https://www.giftipie.me/"); // 예시 이미지 처리
        LocalDate currentDate = LocalDate.now();
        FundingStatus status = requestDto.getEndDate().isBefore(currentDate) ? FundingStatus.FINISHED : FundingStatus.ACTIVE;
        Funding funding = requestDto.toEntity(fundingItem, status);
        funding.setUser(user);
        fundingRepository.save(funding);

        return FundingResponseDto.fromEntity(funding);
    }

    @Transactional(readOnly = true)
    public FundingResponseDto findFunding(Long fundingId) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new NullPointerException("해당 펀딩을 찾을 수 없습니다."));
        return FundingResponseDto.fromEntity(funding);
    }

    @Transactional(readOnly = true)
    public FundingResponseDto getMyFundingInfo(User currentUser) {
        Funding funding = fundingRepository.findByUserIdAndStatus(currentUser.getId(), FundingStatus.ACTIVE);
        if (funding == null) {
            return FundingResponseDto.emptyDto();
        }

        return FundingResponseDto.fromEntity(funding);
    }

    @Transactional(readOnly = true)
    public Page<FundingResponseDto> getActiveFundings(int page, int size, String sortBy, String sortOrder) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy));
        Page<Funding> fundings = fundingRepository.findByStatus(FundingStatus.ACTIVE, pageable);
        return fundings.map(FundingResponseDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<FundingResponseDto> getFinishedFundings(int page, int size, String sortBy, String sortOrder) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy));
        Page<Funding> fundings = fundingRepository.findByStatus(FundingStatus.FINISHED, pageable);
        return fundings.map(FundingResponseDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<FundingResponseDto> getAllFundings(int page, int size, String sortBy, String sortOrder) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy));
        Page<Funding> fundings = fundingRepository.findAll(pageable);
        return fundings.map(FundingResponseDto::fromEntity);
    }

    @Transactional
    public void finishFunding(Long fundingId, User currentUser) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.FUNDING_NOT_FOUND));

        if (!funding.getUser().getId().equals(currentUser.getId())) {
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED_FINISHED_FUNDING);
        }

        funding.setStatus(FundingStatus.FINISHED);
        fundingRepository.save(funding);
    }

    @Transactional
    public FundingResponseDto updateFunding(Long fundingId, User user, FundingUpdateRequestDto requestDto) {
        Funding funding = fundingRepository.findById(fundingId).orElseThrow(
                () -> new BaseException(BaseResponseStatus.FUNDING_NOT_FOUND));

        if (!funding.getUser().getId().equals(user.getId())) {
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED_UPDATE_FUNDING);
        }

        funding.update(requestDto);
        return FundingResponseDto.fromEntity(funding);
    }

    @Transactional
    public void deleteFunding(Long fundingId, User user) {
        Funding funding = fundingRepository.findById(fundingId).orElseThrow(
                () -> new BaseException(BaseResponseStatus.FUNDING_NOT_FOUND));

        if (!funding.getUser().getId().equals(user.getId())) {
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED_DELETE_FUNDING);
        }

        fundingRepository.delete(funding);
    }

    @Transactional(readOnly = true)
    public FundingSummaryResponseDto getFundingSummary() {
        FundingSummary fundingSummary = fundingSummaryRepository.findFirstByOrderByIdAsc().orElse(new FundingSummary());
        return FundingSummaryResponseDto.builder()
                .totalDonationsCount(fundingSummary.getTotalDonationsCount())
                .successfulFundingsCount(fundingSummary.getSuccessfulFundingsCount())
                .totalFundingAmount(fundingSummary.getTotalFundingAmount())
                .build();
    }

    // ---------------------------- OG 태그 메서드 ------------------------------------------


    public FundingItem previewItem(String itemLink) throws IOException {
        log.info("[previewItem] 상품 미리보기");

        Document document = Jsoup.connect(itemLink).timeout(1000).get();
        String itemImage = getMetaTagContent(document, "og:image");
        if (itemImage == null) {
            throw new IOException("링크 상품 이미지를 가져올 수 없습니다.");
        }
        return new FundingItem(itemLink, itemImage);
    }

    private static String getMetaTagContent(Document document, String property) {
        log.info("[getMetaTagContent] 메타 태크에서 상품이미지 가져오기");

        Elements metaTags = document.select("meta[property=" + property + "]");
        if (!metaTags.isEmpty()) {
            return metaTags.first().attr("content");
        }
        return null;
    }
}