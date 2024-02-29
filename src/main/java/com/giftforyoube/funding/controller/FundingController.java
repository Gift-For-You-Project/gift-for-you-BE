package com.giftforyoube.funding.controller;

import com.giftforyoube.funding.dto.*;
import com.giftforyoube.funding.service.FundingService;
import com.giftforyoube.global.exception.BaseException;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.security.UserDetailsImpl;
import com.giftforyoube.user.entity.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/funding")
@Tag(name = "펀딩", description = "펀딩 관련 API")
public class FundingController {

    private final FundingService fundingService;

    // 링크 추가 및 캐시 저장 요청 처리
    @PostMapping("/addLink")
    public ResponseEntity<?> addLinkAndSaveToCache(@RequestBody AddLinkRequestDto requestDto, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        log.info("[addLinkAndSaveToCache] 상품링크: " + requestDto);

        if (userDetails == null) {
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED_TO_ADD_LINK);
        }
        try {
            FundingItemResponseDto fundingItemResponseDto = fundingService.addLinkAndSaveToCache(requestDto, userDetails.getUser().getId());
            return ResponseEntity.ok(fundingItemResponseDto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error adding link: " + e.getMessage());
        }
    }

    // 펀딩 상세 정보 입력 및 DB 저장 요청 처리
    @PostMapping("/create")
    public ResponseEntity<?> createFunding(@RequestBody FundingCreateRequestDto requestDto,@AuthenticationPrincipal UserDetailsImpl userDetails) {
        log.info("[createFunding] 펀딩등록: " + requestDto);

        if(userDetails == null){
            throw new NullPointerException("펀딩 등록을 하려면 로그인을 해야합니다.");
        }
        Long userId = userDetails.getUser().getId();
        try {
            FundingResponseDto responseDto = fundingService.saveToDatabase(requestDto,userId);
            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating funding: " + e.getMessage());
        }
    }

    // 내 펀딩 정보를 조회하는 API
    @GetMapping("/myFunding")
    public ResponseEntity<?> getMyFunding(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            // 로그인하지 않은 사용자가 API를 호출하면 적절한 HTTP 상태 코드와 메시지를 반환
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요한 기능입니다.");
        }
        FundingResponseDto fundingResponseDto = fundingService.getMyFundingInfo(userDetails.getUser());
        return ResponseEntity.ok(fundingResponseDto);
    }


    // Slice - Page 페이지네이션 수정 적용
    @GetMapping("/all")
    public ResponseEntity<Page<FundingResponseDto>> getAllFundings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder
    ){
        log.info("[getAllFundings] 모든 펀딩 리스트 조회 무한스크롤");

        Page<FundingResponseDto> allFundingsPage = fundingService.getAllFundings(page, size, sortBy, sortOrder);
        return ResponseEntity.ok(allFundingsPage);
    }

    @GetMapping("/active")
    public ResponseEntity<Slice<FundingResponseDto>> getActiveFundings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder
    ){
        log.info("[getActiveFundings] 진행중인 펀딩 리스트 조회 무한스크롤");

        Slice<FundingResponseDto> activeFundingsPage = fundingService.getActiveFundings(page, size, sortBy, sortOrder);
        return ResponseEntity.ok(activeFundingsPage);
    }

    // 펀딩 등록시 저장된 마감일 기준으로 현재 종료된 펀딩 [페이지네이션 적용]
    @GetMapping("/finished")
    public ResponseEntity<Slice<FundingResponseDto>> getFinishedFundings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder
    ){
        log.info("[getFinishedFundings] 완료된 펀딩 리스트 조회 무한스크롤");
        Slice<FundingResponseDto> finishedFundingsPage = fundingService.getFinishedFundings(page, size, sortBy, sortBy);
        return ResponseEntity.ok(finishedFundingsPage);
    }

    // D-Day를 포함한 펀딩 상세 페이지
    @GetMapping("/{fundingId}")
    public ResponseEntity<FundingResponseDto> findFunding(@PathVariable Long fundingId, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        log.info("[findFunding] 펀딩 상세 페이지" + fundingId);

        User user = null;
        if (userDetails != null) {
            user = userDetails.getUser();
        }
        // 캐시된 데이터를 사용하여 FundingResponseDto를 얻습니다.
        FundingResponseDto fundingResponseDto = fundingService.findFunding(fundingId);
        log.info("[findFunding] 펀딩 상세 페이지" + fundingResponseDto);

        // 여기에서는 isOwner 값을 동적으로 설정합니다.
        boolean isOwner = user != null && fundingResponseDto.getOwnerId().equals(user.getId());
        fundingResponseDto.setIsOwner(isOwner);

        return ResponseEntity.ok(fundingResponseDto);
    }

    // 펀딩 종료버튼 딸~깍
    @PatchMapping("/{fundingId}/finish")
    public ResponseEntity<?> finishFunding(@PathVariable Long fundingId, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        log.info("[finishFunding] 펀딩 종료하기" + fundingId);

        if(userDetails == null){
            throw new BaseException(BaseResponseStatus.AUTHENTICATION_FAILED);
        }
        // 로그인 필요
//        try {
//            fundingService.finishFunding(fundingId, userDetails.getUser());
//            return ResponseEntity.ok().build();
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error finishing funding: " + e.getMessage());
//        }
        fundingService.finishFunding(fundingId, userDetails.getUser());
        return ResponseEntity.ok().body("해당 펀딩을 성공적으로 종료하였습니다.");
    }

    // 펀딩 수정
    @PatchMapping("/{fundingId}/update")
    public ResponseEntity<FundingResponseDto> updateFunding(@PathVariable Long fundingId,
                                                            @AuthenticationPrincipal UserDetailsImpl userDetails,
                                                            @RequestBody FundingUpdateRequestDto requestDto) {
        if(userDetails == null){
            throw new BaseException(BaseResponseStatus.AUTHENTICATION_FAILED);
        }
        return new ResponseEntity<>(fundingService.updateFunding(fundingId, userDetails.getUser(), requestDto), HttpStatus.OK);
    }

    // 펀딩 삭제
    @DeleteMapping("/{fundingId}")
    public ResponseEntity<?> deleteFunding(@PathVariable Long fundingId, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            throw new BaseException(BaseResponseStatus.AUTHENTICATION_FAILED);
        }
        fundingService.deleteFunding(fundingId, userDetails.getUser());
        return ResponseEntity.ok().body("해당 펀딩을 성공적으로 삭제하였습니다.");
    }

    @GetMapping("/summary")
    public ResponseEntity<FundingSummaryResponseDto> getFundingSummary() {
        FundingSummaryResponseDto summaryResponseDto = fundingService.getFundingSummary();
        return ResponseEntity.ok(summaryResponseDto);
    }
}