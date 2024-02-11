package com.giftforyoube.funding.controller;

import com.giftforyoube.funding.dto.AddLinkRequestDto;
import com.giftforyoube.funding.dto.FundingCreateRequestDto;
import com.giftforyoube.funding.dto.FundingItemResponseDto;
import com.giftforyoube.funding.dto.FundingResponseDto;
import com.giftforyoube.funding.service.FundingService;
import com.giftforyoube.global.exception.BaseException;
import com.giftforyoube.global.exception.BaseResponse;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.security.UserDetailsImpl;
import com.giftforyoube.user.entity.User;
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
public class FundingController {

    private final FundingService fundingService;

    // 링크 추가 및 캐시 저장 요청 처리
//    @PostMapping("/addLink")
//    public ResponseEntity<?> addLinkAndSaveToCache(@RequestBody AddLinkRequestDto requestDto, @AuthenticationPrincipal UserDetailsImpl userDetails) {
//        log.info("[addLinkAndSaveToCache] 상품링크: " + requestDto);
//
//        if (userDetails == null) {
//            throw new BaseException(BaseResponseStatus.UNAUTHORIZED_TO_ADD_LINK);
//        }
//        try {
//            FundingItemResponseDto fundingItemResponseDto = fundingService.addLinkAndSaveToCache(requestDto, userDetails.getUser().getId());
//            return ResponseEntity.ok(fundingItemResponseDto);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error adding link: " + e.getMessage());
//        }
//    }

    @PostMapping("/addLink")
    public ResponseEntity<BaseResponse<FundingItemResponseDto>> addLinkAndSaveToCache(
            @RequestBody AddLinkRequestDto requestDto, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        log.info("[addLinkAndSaveToCache] 상품링크: " + requestDto);

        if (userDetails == null) {
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED_TO_ADD_LINK);
        }
        try {
            FundingItemResponseDto fundingItemResponseDto = fundingService.addLinkAndSaveToCache(requestDto, userDetails.getUser().getId());
            return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.ADD_LINK_SUCCESS, fundingItemResponseDto));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>(BaseResponseStatus.SERVER_ERROR, null));
        }
    }

    // 펀딩 상세 정보 입력 및 DB 저장 요청 처리
//    @PostMapping("/create")
//    public ResponseEntity<?> createFunding(@RequestBody FundingCreateRequestDto requestDto,@AuthenticationPrincipal UserDetailsImpl userDetails) {
//        log.info("[createFunding] 펀딩등록: " + requestDto);
//
//        if(userDetails == null){
//            throw new NullPointerException("펀딩 등록을 하려면 로그인을 해야합니다.");
//        }
//        Long userId = userDetails.getUser().getId();
//        try {
//            FundingResponseDto responseDto = fundingService.saveToDatabase(requestDto,userId);
//            return ResponseEntity.ok(responseDto);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating funding: " + e.getMessage());
//        }
//    }

    @PostMapping("/create")
    public ResponseEntity<BaseResponse<FundingResponseDto>> createFunding(
            @RequestBody FundingCreateRequestDto requestDto, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        log.info("[createFunding] 펀딩등록: " + requestDto);

        if (userDetails == null) {
            throw new BaseException(BaseResponseStatus.AUTHENTICATION_FAILED);
        }
        Long userId = userDetails.getUser().getId();
        try {
            FundingResponseDto responseDto = fundingService.saveToDatabase(requestDto, userId);
            return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.FUNDING_CREAT_SUCCESS, responseDto));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>(BaseResponseStatus.SERVER_ERROR, null));
        }
    }

//    @GetMapping("")
//    public ResponseEntity<Page<FundingResponseDto>> getActiveMainFunding(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "4") int size,
//            @RequestParam(defaultValue = "createdAt") String sortBy,
//            @RequestParam(defaultValue = "desc") String sortOrder
//    ){
//        log.info("[getActiveFunding] 메인페이지 진행중인 펀딩 조회");
//
//        Page<FundingResponseDto> activeFundingsPage = fundingService.getActiveMainFunding(page, size, sortBy, sortOrder);
//        return ResponseEntity.ok(activeFundingsPage);
//    }

    @GetMapping("")
    public ResponseEntity<BaseResponse<Page<FundingResponseDto>>> getActiveMainFunding(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        log.info("[getActiveFunding] 메인페이지 진행중인 펀딩 조회");

        Page<FundingResponseDto> activeFundingsPage = fundingService.getActiveMainFunding(page, size, sortBy, sortOrder);
        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, activeFundingsPage));
    }

    // 펀딩 등록시 저장된 마감일 기준으로 현재 진행중인 펀딩 [페이지네이션]
//    @GetMapping("/active")
//    public ResponseEntity<Slice<FundingResponseDto>> getActiveFundings(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size,
//            @RequestParam(defaultValue = "createdAt") String sortBy,
//            @RequestParam(defaultValue = "desc") String sortOrder
//    ){
//        log.info("[getActiveFundings] 진행중인 펀딩 리스트 조회 무한스크롤");
//
//        Slice<FundingResponseDto> activeFundingsPage = fundingService.getActiveFundings(page, size, sortBy, sortOrder);
//        return ResponseEntity.ok(activeFundingsPage);
//    }

    @GetMapping("/active")
    public ResponseEntity<BaseResponse<Slice<FundingResponseDto>>> getActiveFundings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        log.info("[getActiveFundings] 진행중인 펀딩 리스트 조회 무한스크롤");

        Slice<FundingResponseDto> activeFundingsPage = fundingService.getActiveFundings(page, size, sortBy, sortOrder);
        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, activeFundingsPage));
    }

    // 펀딩 등록시 저장된 마감일 기준으로 현재 종료된 펀딩 [페이지네이션 적용]
//    @GetMapping("/finished")
//    public ResponseEntity<Slice<FundingResponseDto>> getFinishedFundings(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size,
//            @RequestParam(defaultValue = "createdAt") String sortBy,
//            @RequestParam(defaultValue = "desc") String sortOrder
//    ){
//        log.info("[getFinishedFundings] 완료된 펀딩 리스트 조회 무한스크롤");
//        Slice<FundingResponseDto> finishedFundingsPage = fundingService.getFinishedFundings(page, size, sortBy, sortBy);
//        return ResponseEntity.ok(finishedFundingsPage);
//    }

    @GetMapping("/finished")
    public ResponseEntity<BaseResponse<Slice<FundingResponseDto>>> getFinishedFundings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        log.info("[getFinishedFundings] 완료된 펀딩 리스트 조회 무한스크롤");

        Slice<FundingResponseDto> finishedFundingsPage = fundingService.getFinishedFundings(page, size, sortBy, sortOrder);
        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, finishedFundingsPage));
    }

    // D-Day를 포함한 펀딩 상세 페이지
//    @GetMapping("/{fundingId}")
//    public ResponseEntity<FundingResponseDto> findFunding(@PathVariable Long fundingId, @AuthenticationPrincipal UserDetailsImpl userDetails) {
//        log.info("[findFunding] 펀딩 상세 페이지" + fundingId);
//
//        User user = null;
//        if (userDetails != null) {
//            user = userDetails.getUser();
//        }
//        // 캐시된 데이터를 사용하여 FundingResponseDto를 얻습니다.
//        FundingResponseDto fundingResponseDto = fundingService.findFunding(fundingId);
//        log.info("[findFunding] 펀딩 상세 페이지" + fundingResponseDto);
//
//        // 여기에서는 isOwner 값을 동적으로 설정합니다.
//        boolean isOwner = user != null && fundingResponseDto.getOwnerId().equals(user.getId());
//        fundingResponseDto.setIsOwner(isOwner);
//
//        return ResponseEntity.ok(fundingResponseDto);
//    }

    @GetMapping("/{fundingId}")
    public ResponseEntity<BaseResponse<FundingResponseDto>> findFunding(
            @PathVariable Long fundingId, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        log.info("[findFunding] 펀딩 상세 페이지" + fundingId);

        User user = userDetails != null ? userDetails.getUser() : null;
        FundingResponseDto fundingResponseDto = fundingService.findFunding(fundingId);
        boolean isOwner = user != null && fundingResponseDto.getOwnerId().equals(user.getId());
        fundingResponseDto.setIsOwner(isOwner);

        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, fundingResponseDto));
    }

    // 펀딩 종료버튼 딸~깍
//    @PatchMapping("/{fundingId}/finish")
//    public ResponseEntity<?> finishFunding(@PathVariable Long fundingId, @AuthenticationPrincipal UserDetailsImpl userDetails) {
//        log.info("[finishFunding] 펀딩 종료하기" + fundingId);
//
//        if(userDetails == null){
//            throw new NullPointerException("로그인을 해주십쇼");
//        }
//        try {
//            fundingService.finishFunding(fundingId, userDetails.getUser());
//            return ResponseEntity.ok().build();
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error finishing funding: " + e.getMessage());
//        }
//    }

    @PatchMapping("/{fundingId}/finish")
    public ResponseEntity<BaseResponse<Void>> finishFunding(
            @PathVariable Long fundingId, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        log.info("[finishFunding] 펀딩 종료하기" + fundingId);

        if (userDetails == null) {
            throw new BaseException(BaseResponseStatus.AUTHENTICATION_FAILED);
        }
        try {
            fundingService.finishFunding(fundingId, userDetails.getUser());
            return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>(BaseResponseStatus.SERVER_ERROR, null));
        }
    }

    // 펀딩 수정
    @PatchMapping("/{fundingId}/update")
    public ResponseEntity<FundingResponseDto> updateFunding(@PathVariable Long fundingId,
                                                            @AuthenticationPrincipal UserDetailsImpl userDetails,
                                                            @RequestBody FundingCreateRequestDto requestDto) {
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

}