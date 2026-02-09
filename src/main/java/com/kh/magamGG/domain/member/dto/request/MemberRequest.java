package com.kh.magamGG.domain.member.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MemberRequest {
    private String memberName;
    private String memberEmail;
    private String memberPassword;
    private String memberPhone;
    private String memberAddress;
    private String memberRole;

    // 에이전시 회원가입 시 사용
    private String agencyName;

    // 담당자 회원가입 시 사용 (에이전시 코드로 기존 에이전시 조회)
    private String agencyCode;

    // 아티스트 회원가입 시 선택사항 (에이전시 번호)
    private Long agencyNo;
}
