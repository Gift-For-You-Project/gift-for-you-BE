package com.giftforyoube.funding.dto;

import com.giftforyoube.funding.entity.Funding;
import com.giftforyoube.funding.entity.FundingStatus;
import lombok.Builder;
import lombok.Getter;

import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Getter
public class FundingResponseDto {

    private Long id;
    private String itemLink;
    private String itemImage;
    private String itemName;
    private String title;
    private String content;
    private int currentAmount;
    private int targetAmount;
    private boolean publicFlag;
    private LocalDate endDate;
    private String dDay;
    private FundingStatus status;
    private int achievementRate;
    private URL imageFile;

    @Builder
    public FundingResponseDto(Long id, String itemLink, String itemImage, String itemName, String title, String content, int currentAmount, int targetAmount, boolean publicFlag, LocalDate endDate,String dDay,FundingStatus status,URL imageFile) {
        this.id = id;
        this.itemLink = itemLink;
        this.itemImage = itemImage;
        this.itemName = itemName;
        this.title = title;
        this.content = content;
        this.currentAmount = currentAmount;
        this.targetAmount = targetAmount;
        this.publicFlag = publicFlag;
        this.endDate = endDate;
        this.dDay = dDay;
        this.imageFile = imageFile;
        this.status = status;
        this.achievementRate = calculatorAchievementRate(currentAmount,targetAmount);
    }

    // 달성률 계산
    private int calculatorAchievementRate(int currentAmount, int targetAmount) {
        if(targetAmount == 0){
            return 0;
        }
        return (int)Math.round((double) currentAmount / targetAmount * 100);
    }

    public static FundingResponseDto fromEntity(Funding funding) {
        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), funding.getEndDate());
        String dDay = "D-Day";
        if (daysRemaining != 0){
            dDay = (daysRemaining > 0) ? "D-" + daysRemaining : "D+" + Math.abs(daysRemaining);
        }

        return FundingResponseDto.builder()
                .id(funding.getId())
                .itemLink(funding.getItemLink())
                .itemImage(funding.getItemImage())
                .itemName(funding.getItemName())
                .title(funding.getTitle())
                .content(funding.getContent())
                .currentAmount(funding.getCurrentAmount())
                .targetAmount(funding.getTargetAmount())
                .publicFlag(funding.isPublicFlag())
                .endDate(funding.getEndDate())
                .imageFile(funding.getImageFile())
                .dDay(dDay)
                .status(funding.getStatus())
                .build();
    }
}
