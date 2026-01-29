package com.kh.magamGG.domain.health.repository;

import com.kh.magamGG.domain.health.entity.DailyHealthCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DailyHealthCheckRepository extends JpaRepository<DailyHealthCheck, Long> {
    
    /**
     * 회원별 일일 건강 체크 목록 조회
     */
    List<DailyHealthCheck> findByMember_MemberNoOrderByHealthCheckCreatedAtDesc(Long memberNo);
}
