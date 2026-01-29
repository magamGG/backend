package com.kh.magamGG.domain.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponse {
    private Long memberNo;
    private String memberName;
    private String memberEmail;
    private String memberPhone;
    private String memberStatus;
    private String memberRole;
    private String memberProfileImage;
    private String memberProfileBannerImage;
    private Long agencyNo;
    private String agencyCode; // 에이전시 회원가입 시 생성된 코드 반환
    private Long managerNo; // 담당자 번호 (작가가 담당자를 가리키는 경우)
    private LocalDateTime memberCreatedAt;
    private LocalDateTime memberUpdatedAt;
}
