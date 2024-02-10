package com.giftforyoube.donation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReadyDonationRequestDto {

    private String sponsorNickname;
    private String comment;
    private int donation;
}