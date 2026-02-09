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
    /** 오늘 ATTENDANCE 기준 작업 상태: 근무중(마지막 출근), 작업 종료(마지막 퇴근), 작업 시작전(기록 없음) */
    private String todayWorkStatus;
}
