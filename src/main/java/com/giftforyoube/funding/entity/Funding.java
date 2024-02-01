package com.giftforyoube.funding.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.URL;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class Funding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1000)
    private String itemLink;
    private String itemImage;
    private String itemName;
    private String title;
    @Column(length = 1000)
    private String content;
    private int currentAmount;
    private int targetAmount;
    private boolean publicFlag;
    private LocalDate endDate;
    @Enumerated(EnumType.STRING)
    private FundingStatus status;
    private URL imageFile;

    @Builder
    public Funding(String itemLink, String itemImage, String itemName, String title, String content, int currentAmount, int targetAmount, boolean publicFlag, LocalDate endDate,FundingStatus status,URL imageFile) {
        this.itemLink = itemLink;
        this.itemImage = itemImage;
        this.itemName = itemName;
        this.title = title;
        this.content = content;
        this.currentAmount = currentAmount;
        this.targetAmount = targetAmount;
        this.publicFlag = publicFlag;
        this.endDate = endDate;
        this.status = status;
        this.imageFile = imageFile;
    }
}
