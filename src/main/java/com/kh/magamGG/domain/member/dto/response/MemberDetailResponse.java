package com.kh.magamGG.domain.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberDetailResponse {
    private List<String> currentProjects; // 참여 중인 프로젝트 목록
    private List<String> participatedProjects; // 참여한 프로젝트 목록
    private List<String> myWorks; // 내 작품 목록 (작가용)
    private List<String> serializingWorks; // 연재중인 작품 목록 (작가용)
    private List<ManagedArtistInfo> managedArtists; // 담당 작가 목록 (담당자용)
    private HealthCheckInfo healthCheck; // 건강 체크 정보

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManagedArtistInfo {
        private Long id;
        private String name;
        private String role;
        private String position;
        private String email;
        private String phone;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthCheckInfo {
        private String date;
        private String condition;
        private Integer sleepHours;
        private Integer discomfortLevel;
        private String memo;
    }
}
