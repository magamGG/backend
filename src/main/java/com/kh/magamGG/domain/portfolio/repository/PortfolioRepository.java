package com.kh.magamGG.domain.portfolio.repository;

import com.kh.magamGG.domain.portfolio.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    List<Portfolio> findByMember_MemberNo(Long memberNo);

    List<Portfolio> findByMember_MemberNoAndPortfolioStatus(Long memberNo, String portfolioStatus);

    Optional<Portfolio> findFirstByMember_MemberNoAndPortfolioStatusOrderByPortfolioCreatedAtDesc(Long memberNo, String portfolioStatus);
}
