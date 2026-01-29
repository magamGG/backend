package com.kh.magamGG.domain.health.repository;

import com.kh.magamGG.domain.health.entity.DailyHealthCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DailyHealthCheckRepository extends JpaRepository<DailyHealthCheck, Long> {
    List<DailyHealthCheck> findByMember_MemberNoOrderByHealthCheckCreatedAtDesc(Long memberNo);
    Optional<DailyHealthCheck> findFirstByMember_MemberNoOrderByHealthCheckCreatedAtDesc(Long memberNo);
}
