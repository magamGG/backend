package com.kh.magamGG.domain.auth.service;

import com.kh.magamGG.domain.auth.dto.request.LoginRequest;
import com.kh.magamGG.domain.auth.dto.response.LoginResponse;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.global.exception.InvalidCredentialsException;
import com.kh.magamGG.global.exception.MemberNotFoundException;
import com.kh.magamGG.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthService {
    
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    
    public LoginResponse login(LoginRequest request) {
        // 이메일로 회원 조회
        Member member = memberRepository.findByMemberEmail(request.getMemberEmail())
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 이메일입니다."));
        
        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getMemberPassword(), member.getMemberPassword())) {
            throw new InvalidCredentialsException("비밀번호가 일치하지 않습니다.");
        }
        
        // 계정 상태 확인
        if (!"ACTIVE".equals(member.getMemberStatus())) {
            throw new InvalidCredentialsException("비활성화된 계정입니다.");
        }
        
        // JWT 토큰 생성
        String token = jwtTokenProvider.generateToken(member.getMemberNo(), member.getMemberEmail());
        
        // Agency 번호 추출 (null일 수 있음)
        Long agencyNo = member.getAgency() != null ? member.getAgency().getAgencyNo() : null;
        
        log.info("로그인 성공: {} ({})", member.getMemberName(), member.getMemberEmail());
        
        return LoginResponse.builder()
                .token(token)
                .memberNo(member.getMemberNo())
                .memberName(member.getMemberName())
                .memberRole(member.getMemberRole())
                .agencyNo(agencyNo)
                .build();
    }
}
