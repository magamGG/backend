package com.kh.magamGG.domain.member.service;

import com.kh.magamGG.domain.agency.entity.Agency;
import com.kh.magamGG.domain.agency.repository.AgencyRepository;
import com.kh.magamGG.domain.agency.util.AgencyCodeGenerator;
import com.kh.magamGG.domain.member.dto.request.MemberRequest;
import com.kh.magamGG.domain.member.dto.response.MemberResponse;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.global.exception.AgencyNotFoundException;
import com.kh.magamGG.global.exception.DuplicateAgencyCodeException;
import com.kh.magamGG.global.exception.DuplicateEmailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberServiceImpl implements MemberService {
    
    private final MemberRepository memberRepository;
    private final AgencyRepository agencyRepository;
    private final AgencyCodeGenerator agencyCodeGenerator;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    @Transactional
    public MemberResponse register(MemberRequest request) {
        // 이메일 중복 체크
        if (memberRepository.existsByMemberEmail(request.getMemberEmail())) {
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
        }
        
        Agency agency = null;
        String agencyCode = null;
        
        // 에이전시 관리자 회원가입
        if ("에이전시 관리자".equals(request.getMemberRole())) {
            // 에이전시명 중복 체크
            if (request.getAgencyName() != null && agencyRepository.existsByAgencyName(request.getAgencyName())) {
                throw new DuplicateAgencyCodeException("이미 사용 중인 에이전시명입니다.");
            }
            
            // 11자리 랜덤 에이전시 코드 생성
            agencyCode = agencyCodeGenerator.generateUniqueAgencyCode();
            
            // AGENCY 테이블에 에이전시 정보 저장
            agency = Agency.builder()
                    .agencyName(request.getAgencyName())
                    .agencyCode(agencyCode)
                    .agencyLeave(15) // 기본값
                    .build();
            
            agency = agencyRepository.save(agency);
            log.info("에이전시 생성 완료: {} (코드: {})", request.getAgencyName(), agencyCode);
        }
        // 담당자 회원가입
        else if ("담당자".equals(request.getMemberRole())) {
            // 담당자는 선택적으로 에이전시에 속할 수 있음 (비소속 허용)
            if (request.getAgencyCode() != null && !request.getAgencyCode().isEmpty()) {
                // 에이전시 코드가 제공된 경우에만 에이전시 조회
                agency = agencyRepository.findByAgencyCode(request.getAgencyCode())
                        .orElseThrow(() -> new AgencyNotFoundException("존재하지 않는 에이전시 코드입니다."));
            }
            // agencyCode가 null이거나 비어있으면 담당자가 독립적으로 가입하는 경우 (비소속)
        }
        // 아티스트 회원가입
        else {
            // 아티스트는 선택적으로 에이전시에 속할 수 있음
            if (request.getAgencyNo() != null) {
                agency = agencyRepository.findById(request.getAgencyNo())
                        .orElseThrow(() -> new AgencyNotFoundException("존재하지 않는 에이전시입니다."));
            }
            // agency가 null이면 아티스트가 독립적으로 가입하는 경우
        }
        
        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getMemberPassword());
        
        // MEMBER 테이블에 회원 정보 저장
        Member member = Member.builder()
                .agency(agency)
                .memberName(request.getMemberName())
                .memberEmail(request.getMemberEmail())
                .memberPassword(encodedPassword)
                .memberPhone(request.getMemberPhone())
                .memberRole(request.getMemberRole())
                .memberStatus("ACTIVE")
                .memberCreatedAt(LocalDateTime.now())
                .build();
        
        member = memberRepository.save(member);
        log.info("회원가입 완료: {} ({})", member.getMemberName(), member.getMemberEmail());
        
        // 응답 생성
        return new MemberResponse(
                member.getMemberNo(),
                member.getMemberName(),
                member.getMemberEmail(),
                member.getMemberRole(),
                agency != null ? agency.getAgencyNo() : null,
                agencyCode // 에이전시 회원가입 시 생성된 코드 반환
        );
    }
}
