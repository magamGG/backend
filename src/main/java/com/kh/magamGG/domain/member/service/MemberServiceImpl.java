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
import java.util.List;
import java.util.stream.Collectors;

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
            // 담당자는 항상 독립적으로 가입 (에이전시 연결 없음)
            agency = null;
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
        return MemberResponse.builder()
                .memberNo(member.getMemberNo())
                .memberName(member.getMemberName())
                .memberEmail(member.getMemberEmail())
                .memberPhone(member.getMemberPhone())
                .memberStatus(member.getMemberStatus())
                .memberRole(member.getMemberRole())
                .memberProfileImage(member.getMemberProfileImage())
                .memberProfileBannerImage(member.getMemberProfileBannerImage())
                .agencyNo(agency != null ? agency.getAgencyNo() : null)
                .agencyCode(agencyCode)
                .memberCreatedAt(member.getMemberCreatedAt())
                .memberUpdatedAt(member.getMemberUpdatedAt())
                .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> getMembersByAgencyNo(Long agencyNo) {
        List<Member> members = memberRepository.findByAgency_AgencyNo(agencyNo);
        
        return members.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    private MemberResponse convertToResponse(Member member) {
        return MemberResponse.builder()
                .memberNo(member.getMemberNo())
                .memberName(member.getMemberName())
                .memberEmail(member.getMemberEmail())
                .memberPhone(member.getMemberPhone())
                .memberStatus(member.getMemberStatus())
                .memberRole(member.getMemberRole())
                .memberProfileImage(member.getMemberProfileImage())
                .memberProfileBannerImage(member.getMemberProfileBannerImage())
                .agencyNo(member.getAgency() != null ? member.getAgency().getAgencyNo() : null)
                .agencyCode(member.getAgency() != null ? member.getAgency().getAgencyCode() : null)
                .memberCreatedAt(member.getMemberCreatedAt())
                .memberUpdatedAt(member.getMemberUpdatedAt())
                .build();
    }
}
