package com.kh.magamGG.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;  // 기존 프론트엔드 호환성 유지 (Access Token)
    private String accessToken;  // 새 필드 (명시적)
    private String refreshToken;  // 새 필드
    private Long memberNo;
    private String memberName;
    private String memberRole;
    private Long agencyNo;
    private String memberProfileImage;  // 프로필 이미지 추가
}
