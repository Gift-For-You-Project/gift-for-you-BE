package com.giftforyoube.funding.service;

import com.giftforyoube.funding.dto.FundingCreateRequestDto;
import com.giftforyoube.funding.dto.FundingResponseDto;
import com.giftforyoube.funding.entity.Funding;
import com.giftforyoube.funding.entity.FundingItem;
import com.giftforyoube.funding.entity.FundingStatus;
import com.giftforyoube.funding.repository.FundingRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;

@Service
@Getter
@RequiredArgsConstructor
public class FundingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final FundingRepository fundingRepository;
    private final ImageS3Service imageS3Service;

    private static final int TIMEOUT = 10000; // 10초

    public void saveToCache(FundingItem fundingItem) {
        redisTemplate.opsForValue().set("cachedFundingItem", fundingItem);
    }

    public FundingItem getCachedFundingProduct() {
        return (FundingItem) redisTemplate.opsForValue().get("cachedFundingItem");
    }

    public FundingItem previewItem(String itemLink) throws IOException {
        Document document = Jsoup.connect(itemLink).timeout(TIMEOUT).get();
        String itemImage = getMetaTagContent(document, "og:image");
        if (itemImage == null) {
            throw new IOException("Cannot fetch item image.");
        }
        return FundingItem.builder()
                .itemLink(itemLink)
                .itemImage(itemImage)
                .build();
    }

//    @Transactional
//    public FundingResponseDto saveToDatabase(FundingCreateRequestDto requestDto) {
//        FundingItem fundingItem = getCachedFundingProduct();
//        if (fundingItem == null) {
//            throw new IllegalStateException("No cached funding item found.");
//        }
//        LocalDate currentDate = LocalDate.now();
//        FundingStatus status = requestDto.getEndDate().isBefore(currentDate) ? FundingStatus.FINISHED : FundingStatus.ACTIVE;
//        Funding funding = requestDto.toEntity(fundingItem,status);
//        fundingRepository.save(funding);
//        clearCache();
//        return FundingResponseDto.fromEntity(funding);
//    }

    //S3 이미지 업로드방식
    @Transactional
    public FundingResponseDto saveToDatabase(FundingCreateRequestDto requestDto, String mainImage) throws IOException {
        LocalDate currentDate = LocalDate.now();
        FundingStatus status = requestDto.getEndDate().isBefore(currentDate) ? FundingStatus.FINISHED : FundingStatus.ACTIVE;
        Funding funding;
        if (mainImage != null) {
            URL mainImageUrl = new URL(mainImage);
            funding = requestDto.toEntity(mainImageUrl, status);
        } else {
            funding = requestDto.toEntity(null, status);
        }
        fundingRepository.save(funding);
        clearCache();
        return FundingResponseDto.fromEntity(funding);
    }

    public void clearCache() {
        redisTemplate.delete("cachedFundingItem");
    }

    private static String getMetaTagContent(Document document, String property) {
        Element metaTag = document.select("meta[property=" + property + "]").first();
        return (metaTag != null) ? metaTag.attr("content") : null;
    }

    @Transactional(readOnly = true)
    public FundingResponseDto findFunding(Long fundingId) {
        Funding findFunding = fundingRepository.findById(fundingId).orElseThrow(
                () -> new NullPointerException("해당 펀딩을 찾을 수 없습니다.")
        );
        return FundingResponseDto.fromEntity(findFunding);
    }

    @Transactional(readOnly = true)
    public List<Funding> getActiveFundings() {
        LocalDate currentDate = LocalDate.now();
        return fundingRepository.findByEndDateGreaterThanEqualAndStatus(currentDate, FundingStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<Funding> getFinishedFunding() {
        LocalDate currentDate = LocalDate.now();
        return fundingRepository.findByEndDateLessThanAndStatus(currentDate, FundingStatus.FINISHED);
    }

    @Transactional
    public void finishFunding(Long fundingId) {
        Funding funding = fundingRepository.findById(fundingId).orElseThrow(
                () -> new IllegalArgumentException("해당 펀딩을 찾을 수 없습니다.")
        );
        funding.setStatus(FundingStatus.FINISHED);
    }
}
