package com.giftforyoube.funding.controller;

import com.giftforyoube.funding.dto.AddLinkRequestDto;
import com.giftforyoube.funding.dto.FundingCreateRequestDto;
import com.giftforyoube.funding.dto.FundingResponseDto;
import com.giftforyoube.funding.entity.Funding;
import com.giftforyoube.funding.entity.FundingItem;
import com.giftforyoube.funding.service.FundingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/funding")
public class FundingController {

    private final FundingService fundingService;

    // 링크 추가 및 캐시 저장 요청 처리
    @PostMapping("/addLink")
    public ResponseEntity<?> addLinkAndSaveToCache(@RequestBody AddLinkRequestDto requestDto) {
        try {
            FundingItem fundingItem = fundingService.previewItem(requestDto.getItemLink());
            fundingService.saveToCache(fundingItem);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error adding link: " + e.getMessage());
        }
    }

    // 펀딩 상세 정보 입력 및 DB 저장 요청 처리
//    @PostMapping("/create")
//    public ResponseEntity<?> createFunding(@RequestBody FundingCreateRequestDto requestDto) {
//        try {
//            FundingResponseDto responseDto = fundingService.saveToDatabase(requestDto);
//            return ResponseEntity.ok(responseDto);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating funding: " + e.getMessage());
//        }
//    }
    // 펀딩 상세 정보 입력 및 DB 저장 요청 처리(이미지 업로드 방식)
    @PostMapping("/create")
    public ResponseEntity<?> createFunding(
            @RequestPart(value = "imageFile") MultipartFile imageFile,
            @RequestBody FundingCreateRequestDto requestDto
    ) {
        try {
            FundingResponseDto responseDto = fundingService.saveToDatabase(requestDto, imageFile);
            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating funding: " + e.getMessage());
        }
    }

    // 캐시에서 아이템 삭제 요청 처리
    @DeleteMapping("/clearCache")
    public ResponseEntity<?> clearCache() {
        try {
            fundingService.clearCache();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error clearing cache: " + e.getMessage());
        }
    }

    // 펀딩 등록시 저장된 마감일 기준으로 현재 진행중인 펀딩
    @GetMapping("/active")
    public ResponseEntity<List<Funding>> getActiveFundings(){
        List<Funding> activeFundings = fundingService.getActiveFundings();
        return ResponseEntity.ok(activeFundings);
    }

    // 펀딩 등록시 저장된 마감일 기준으로 현재 종료된 펀딩
    @GetMapping("/finished")
    public ResponseEntity<List<Funding>> getFinishedFundings(){
        List<Funding> finishedFundings = fundingService.getFinishedFunding();
        return ResponseEntity.ok(finishedFundings);
    }

    // D-Day를 포함한 펀딩 상세 페이지
    @GetMapping("/{fundingId}")
    public FundingResponseDto findFunding(@PathVariable Long fundingId){
        return fundingService.findFunding(fundingId);
    }

    @PatchMapping("/{fundingId}/finish")
    public ResponseEntity<?> finishFunding(@PathVariable Long fundingId) {
        try {
            fundingService.finishFunding(fundingId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error finishing funding: " + e.getMessage());
        }
    }

}
