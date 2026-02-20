package com.kh.magamGG.domain.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kh.magamGG.domain.auth.dto.response.LoginResponse;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.global.exception.OAuthRegistrationRequiredException;
import com.kh.magamGG.global.security.JwtTokenProvider;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GoogleOAuthService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${google.oauth.client-id}")
    private String clientId;
    
    @Value("${google.oauth.client-secret}")
    private String clientSecret;
    
    @Value("${google.oauth.redirect-uri}")
    private String redirectUri;
    
    @Value("${google.oauth.scope}")
    private String scope;
    
    /**
     * Google OAuth 인증 URL 생성
     */
    public String getAuthorizationUrl() {
        String baseUrl = "https://accounts.google.com/o/oauth2/v2/auth";
        
        return UriComponentsBuilder.fromUriString(baseUrl)
            .queryParam("client_id", clientId)
            .queryParam("redirect_uri", redirectUri)
            .queryParam("response_type", "code")
            .queryParam("scope", scope)
            .queryParam("access_type", "offline")
            .queryParam("prompt", "consent")
            .build()
            .toUriString();
    }
    
    /**
     * Google OAuth 콜백 처리 (인증 코드로 토큰 교환 및 로그인)
     */
    @Transactional
    public LoginResponse handleCallback(String code) {
        try {
            // 1. 인증 코드로 액세스 토큰 교환
            String accessToken = exchangeCodeForToken(code);
            
            // 2. 액세스 토큰으로 사용자 정보 조회
            GoogleUserInfo userInfo = getUserInfo(accessToken);
            
            // 3. 회원 조회 또는 생성
            Member member = findOrCreateMember(userInfo);
            
            // 4. Access Token 발급
            String accessTokenJwt = jwtTokenProvider.generateAccessToken(
                member.getMemberNo(), 
                member.getMemberEmail()
            );
            
            // 5. Refresh Token 발급
            String refreshTokenJwt = jwtTokenProvider.generateRefreshToken(member.getMemberNo());
            
            // 6. Refresh Token 해시 생성
            String tokenHash = jwtTokenProvider.hashToken(refreshTokenJwt);
            
            // 7. Token Family UUID 생성
            String tokenFamily = UUID.randomUUID().toString();
            
            // 8. 만료 시간 계산
            LocalDateTime expiryDate = LocalDateTime.now()
                .plusSeconds(jwtTokenProvider.getRefreshExpiration() / 1000);
            
            // 9. Refresh Token DB 저장 (해시만 저장) - AuthService와 동일한 로직
            RefreshToken refreshTokenEntity = RefreshToken.builder()
                .memberNo(member.getMemberNo())
                .refreshTokenHash(tokenHash)
                .refreshTokenFamily(tokenFamily)
                .refreshTokenIsRevoked("F")
                .refreshTokenExpiresAt(expiryDate)
                .refreshTokenCreatedAt(LocalDateTime.now())
                .build();
            
            refreshTokenRepository.save(refreshTokenEntity);
            
            log.info("Google 로그인 성공: {} ({})", member.getMemberName(), member.getMemberEmail());
            
            // 10. Agency 번호 추출
            Long agencyNo = member.getAgency() != null ? member.getAgency().getAgencyNo() : null;
            
            return LoginResponse.builder()
                .token(accessTokenJwt)  // 기존 프론트엔드와 호환성 유지
                .accessToken(accessTokenJwt)
                .refreshToken(refreshTokenJwt)
                .memberNo(member.getMemberNo())
                .memberName(member.getMemberName())
                .memberRole(member.getMemberRole())
                .agencyNo(agencyNo)
                .build();
                
        } catch (OAuthRegistrationRequiredException e) {
            // OAuth 회원가입 필요 예외는 그대로 전파 (AuthController에서 처리)
            throw e;
        } catch (Exception e) {
            log.error("Google OAuth 콜백 처리 실패", e);
            throw new RuntimeException("Google 로그인 처리 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 인증 코드를 액세스 토큰으로 교환
     */
    private String exchangeCodeForToken(String code) {
        try {
            String tokenUrl = "https://oauth2.googleapis.com/token";
            
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("code", code);
            params.add("grant_type", "authorization_code");
            params.add("redirect_uri", redirectUri);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);
            
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new RuntimeException("토큰 교환 실패");
            }
            
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            return jsonNode.get("access_token").asText();
            
        } catch (Exception e) {
            log.error("토큰 교환 실패: code={}", code, e);
            throw new RuntimeException("토큰 교환 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 액세스 토큰으로 Google 사용자 정보 조회
     */
    private GoogleUserInfo getUserInfo(String accessToken) {
        try {
            String userInfoUrl = "https://www.googleapis.com/oauth2/v2/userinfo";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                userInfoUrl,
                HttpMethod.GET,
                request,
                String.class
            );
            
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new RuntimeException("사용자 정보 조회 실패");
            }
            
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            
            return GoogleUserInfo.builder()
                .email(jsonNode.get("email").asText())
                .name(jsonNode.get("name").asText())
                .picture(jsonNode.has("picture") ? jsonNode.get("picture").asText() : null)
                .build();
                
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패: accessToken={}", accessToken, e);
            throw new RuntimeException("사용자 정보 조회 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 회원 조회 또는 생성
     */
    @Transactional
    private Member findOrCreateMember(GoogleUserInfo userInfo) {
        Optional<Member> existingMember = memberRepository.findByMemberEmail(userInfo.getEmail());
        
        if (existingMember.isPresent()) {
            Member member = existingMember.get();
            // 계정 상태 확인
            if (!"ACTIVE".equals(member.getMemberStatus()) && !"활성".equals(member.getMemberStatus())) {
                throw new RuntimeException("비활성화된 계정입니다.");
            }
            return member;
        }
        
        // 신규 회원인 경우 회원가입 페이지로 리디렉션
        throw new OAuthRegistrationRequiredException(userInfo.getEmail(), userInfo.getName(), "google");
    }
    
    /**
     * Google 사용자 정보 DTO
     */
    @Data
    @Builder
    private static class GoogleUserInfo {
        private String email;
        private String name;
        private String picture;
    }
}


