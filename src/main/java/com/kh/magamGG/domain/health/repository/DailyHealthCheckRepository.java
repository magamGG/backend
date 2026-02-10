package com.kh.magamGG.domain.health.repository;

import com.kh.magamGG.domain.health.entity.DailyHealthCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DailyHealthCheckRepository extends JpaRepository<DailyHealthCheck, Long> {

    /**
     * 회원별 일일 건강 체크 목록 조회
     */
    List<DailyHealthCheck> findByMember_MemberNoOrderByHealthCheckCreatedAtDesc(Long memberNo);
    Optional<DailyHealthCheck> findFirstByMember_MemberNoOrderByHealthCheckCreatedAtDesc(Long memberNo);

    /**
     * 회원의 해당 날짜(당일) 데일리 체크 1건 조회 (오늘 날짜 기준)
     */
    Optional<DailyHealthCheck> findFirstByMember_MemberNoAndHealthCheckCreatedAtBetweenOrderByHealthCheckCreatedAtDesc(
            Long memberNo, LocalDateTime startInclusive, LocalDateTime endInclusive);
}
