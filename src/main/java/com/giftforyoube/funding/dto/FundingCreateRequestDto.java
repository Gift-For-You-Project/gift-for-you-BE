package com.giftforyoube.funding.dto;

import com.giftforyoube.funding.entity.Funding;
import com.giftforyoube.funding.entity.FundingItem;
import com.giftforyoube.funding.entity.FundingStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.time.LocalDate;

@Getter
@Setter
public class FundingCreateRequestDto {
    private String itemName;
    private String title;
    private String content;
    private int targetAmount;
    private boolean publicFlag;
    private LocalDate endDate;
    private MultipartFile imageFile;

    // 생성자, Getter, Setter 등 필요한 메서드를 추가할 수 있습니다.

    public FundingCreateRequestDto() {
        // 기본 생성자
    }

    public Funding toEntity(FundingItem fundingItem, FundingStatus status) {
        return Funding.builder()
                .itemLink(fundingItem.getItemLink())
                .itemImage(fundingItem.getItemImage())
                .itemName(this.itemName)
                .title(this.title)
                .content(this.content)
                .currentAmount(0)
                .targetAmount(this.targetAmount)
                .publicFlag(this.publicFlag)
                .endDate(this.getEndDate())
                .status(status)
                .build();
    }

    //이미지 업로드
    public Funding toEntity(URL imageFile, FundingStatus status) {
        return Funding.builder()
                .itemName(this.itemName)
                .title(this.title)
                .content(this.content)
                .currentAmount(0)
                .targetAmount(this.targetAmount)
                .publicFlag(this.publicFlag)
                .endDate(this.getEndDate())
                .imageFile(imageFile)
                .status(status)
                .build();
    }
}