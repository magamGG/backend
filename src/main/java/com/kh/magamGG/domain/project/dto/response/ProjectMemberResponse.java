package com.kh.magamGG.domain.project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로젝트 멤버 조회 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMemberResponse {

    private Long projectMemberNo;  // PROJECT_MEMBER.PROJECT_MEMBER_NO (카드 담당자 지정 시 사용)
    private Long memberNo;
    private String memberName;
    private String memberEmail;
    private String memberPhone;
    private String projectMemberRole;  // 담당자 / 작가 / 어시스트 (PROJECT_MEMBER)
    private String memberRole;         // MEMBER.MEMBER_ROLE (어시스트 - 채색 등 세부 역할)
    private String memberProfileImage;
    private String memberStatus;  // 활성 / 휴면
    /** 오늘 근태 상태: 작업중(마지막 기록 출근) / 작업 종료(마지막 기록 퇴근) / 작업 시작 전(오늘 출근 없음) */
    private String todayAttendanceStatus;
}
