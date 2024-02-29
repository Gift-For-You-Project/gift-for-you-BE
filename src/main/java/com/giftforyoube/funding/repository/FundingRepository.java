package com.giftforyoube.funding.repository;

import com.giftforyoube.funding.entity.Funding;
import com.giftforyoube.funding.entity.FundingStatus;
import com.giftforyoube.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FundingRepository extends JpaRepository<Funding, Long> {
    Page<Funding> findAllPageByStatus(FundingStatus status, Pageable pageable);
    @Query("SELECT f FROM Funding f")
    Page<Funding> findById(Pageable pageable);
    Page<Funding> findAll(Pageable pageable);
    @Query("SELECT f FROM Funding f WHERE f.publicFlag = true")
    Page<Funding> findAllAndPublicFlagTrue(Pageable pageable);
    Page<Funding> findByStatus(FundingStatus fundingStatus, Pageable pageable);
    @Query("SELECT f FROM Funding f WHERE f.status = :status AND f.publicFlag = true")
    Slice<Funding> findByStatusAndPublicFlagTrue(@Param("status") FundingStatus fundingStatus, Pageable pageable);
    List<Funding> findByEndDateLessThanEqualAndStatus(LocalDate currentDate, FundingStatus fundingStatus);
  
    Funding findByUserIdAndStatus(Long userId, FundingStatus fundingStatus);
}