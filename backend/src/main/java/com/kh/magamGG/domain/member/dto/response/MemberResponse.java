package com.kh.magamGG.domain.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponse {
    private Long memberNo;
    private String memberName;
    private String memberEmail;
    private String memberRole;
    private Long agencyNo;
    private String agencyCode; // 에이전시 회원가입 시 생성된 코드 반환
}
