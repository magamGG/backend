package com.kh.magamGG.domain.health.service;

import com.kh.magamGG.domain.health.dto.request.DailyHealthCheckRequest;
import com.kh.magamGG.domain.health.entity.DailyHealthCheck;
import com.kh.magamGG.domain.health.repository.DailyHealthCheckRepository;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.global.exception.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 일일 건강 체크 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DailyHealthCheckService {
    
    private final DailyHealthCheckRepository dailyHealthCheckRepository;
    private final MemberRepository memberRepository;
    
    /**
     * 일일 건강 체크 등록
     * @param request 건강 체크 정보
     * @param memberNo 회원 번호
     * @return 저장된 건강 체크 엔티티
     */
    @Transactional
    public DailyHealthCheck createDailyHealthCheck(DailyHealthCheckRequest request, Long memberNo) {
        // 회원 조회
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다."));
        
        // DailyHealthCheck 엔티티 생성 (nullable 필드 null-safe)
        DailyHealthCheck dailyHealthCheck = new DailyHealthCheck();
        dailyHealthCheck.setMember(member);
        dailyHealthCheck.setHealthCondition(request.getHealthCondition() != null ? request.getHealthCondition() : null);
        dailyHealthCheck.setSleepHours(request.getSleepHours());
        dailyHealthCheck.setDiscomfortLevel(request.getDiscomfortLevel() != null ? request.getDiscomfortLevel() : 0);
        dailyHealthCheck.setHealthCheckNotes(request.getHealthCheckNotes());
        dailyHealthCheck.setHealthCheckCreatedAt(LocalDateTime.now());
        
        // 저장 (DAILY_HEALTH_CHECK 테이블에 INSERT)
        DailyHealthCheck saved = dailyHealthCheckRepository.save(dailyHealthCheck);
        
        log.info("일일 건강 체크 등록 완료: 회원번호={}, 건강체크번호={}", memberNo, saved.getDailyHealthNo());
        
        return saved;
    }
}
