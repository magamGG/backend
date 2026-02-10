package com.kh.magamGG.domain.agency.service;

import com.kh.magamGG.domain.agency.dto.request.JoinRequestRequest;
import com.kh.magamGG.domain.agency.dto.request.UpdateHealthScheduleRequest;
import com.kh.magamGG.domain.agency.dto.response.AgencyDashboardMetricsResponse;
import com.kh.magamGG.domain.agency.dto.response.ArtistDistributionResponse;
import com.kh.magamGG.domain.agency.dto.response.AttendanceDistributionResponse;
import com.kh.magamGG.domain.agency.dto.response.ComplianceTrendResponse;
import com.kh.magamGG.domain.agency.dto.response.AgencyDeadlineCountResponse;
import com.kh.magamGG.domain.agency.dto.response.AgencyHealthScheduleResponse;
import com.kh.magamGG.domain.agency.dto.response.AgencyUnscreenedListResponse;
import com.kh.magamGG.domain.agency.dto.response.HealthDistributionResponse;
import com.kh.magamGG.domain.agency.dto.response.HealthMonitoringDetailResponse;
import com.kh.magamGG.domain.agency.dto.response.JoinRequestResponse;
import com.kh.magamGG.domain.agency.entity.Agency;

import java.util.List;

public interface AgencyService {
    JoinRequestResponse createJoinRequest(JoinRequestRequest request, Long memberNo);
    List<JoinRequestResponse> getJoinRequests(Long agencyNo);
    JoinRequestResponse approveJoinRequest(Long newRequestNo);
    JoinRequestResponse rejectJoinRequest(Long newRequestNo, String rejectionReason);
    
    /**
     * 회원의 대기 중인 가입 요청 조회 (회원별 가입 요청 상태 확인용)
     * @param memberNo 회원 번호
     * @return 대기 중인 가입 요청이 있으면 JoinRequestResponse, 없으면 null
     */
    JoinRequestResponse getMyPendingJoinRequest(Long memberNo);

    /**
     * 에이전시 정보 조회
     */
    Agency getAgency(Long agencyNo);

    /**
     * 회사 코드로 에이전시 조회
     */
    Agency getAgencyByCode(String agencyCode);

    /**
     * 에이전시 소속명(스튜디오) 수정
     */
    void updateAgencyName(Long agencyNo, String agencyName);

    /**
     * 에이전시 기본 연차(agencyLeave) 수정
     */
    void updateAgencyLeave(Long agencyNo, Integer agencyLeave);

    /**
     * 에이전시 대시보드 메트릭 조회 (평균 마감 준수율, 활동 작가, 진행 프로젝트)
     */
    AgencyDashboardMetricsResponse getDashboardMetrics(Long agencyNo);

    /**
     * 평균 마감 준수율 추이 (월별 + 전월 대비)
     */
    ComplianceTrendResponse getComplianceTrend(Long agencyNo);

    /**
     * 작품별 아티스트 분포도
     */
    ArtistDistributionResponse getArtistDistribution(Long agencyNo);

    /**
     * 금일 출석 현황 (출근, 재택근무, 휴가, 워케이션, 미출석)
     */
    AttendanceDistributionResponse getAttendanceDistribution(Long agencyNo);

    /**
     * 건강 인원 분포 (위험, 주의, 정상)
     */
    HealthDistributionResponse getHealthDistribution(Long agencyNo);

    /**
     * 검진 모니터링 상세 목록 (정신/신체 타입별 회원별 점수·상태·최근 검사일)
     * @param type "mental" | "physical"
     */
    HealthMonitoringDetailResponse getHealthMonitoringDetail(Long agencyNo, String type);

    /**
     * 에이전시 건강 검진 일정 (HEALTH_SURVEY 생성일·주기 기반 다음 검진 예정일)
     * 정신/신체 동일 주기 사용
     */
    AgencyHealthScheduleResponse getAgencyHealthSchedule(Long agencyNo);

    /**
     * 에이전시 건강 검진 설정 수정 (HEALTH_SURVEY period, cycle 업데이트)
     */
    void updateAgencyHealthSchedule(Long agencyNo, UpdateHealthScheduleRequest request);

    /**
     * 에이전시 미검진 인원 목록 (정신/신체 중 하나라도 미검진이면 포함)
     * status: BOTH, MENTAL_ONLY, PHYSICAL_ONLY
     */
    AgencyUnscreenedListResponse getAgencyUnscreenedList(Long agencyNo);

    /**
     * 담당자(memberNo)의 배정 작가(ARTIST_ASSIGNMENT)만 대상으로 한 건강 인원 분포 (정신/신체 도넛용)
     */
    HealthDistributionResponse getHealthDistributionForManager(Long memberNo);

    /**
     * 담당자 소속 에이전시 기준 건강 검진 일정 (다음 검진 예정일 등)
     */
    AgencyHealthScheduleResponse getHealthScheduleForManager(Long memberNo);

    /**
     * 담당자 배정 작가 중 미검진 인원 목록
     */
    AgencyUnscreenedListResponse getUnscreenedListForManager(Long memberNo);

    /**
     * 담당자 배정 작가 검진 모니터링 상세 (정신/신체 타입별, 점수·상태·최근 검사일)
     */
    HealthMonitoringDetailResponse getHealthMonitoringDetailForManager(Long memberNo, String type);

    /**
     * 에이전시 마감 임박 현황 (담당자 관리 프로젝트의 업무 기준, 오늘~4일 후 5개 집계)
     */
    List<AgencyDeadlineCountResponse.DeadlineItem> getAgencyDeadlineCounts(Long agencyNo);

    /**
     * 미검진 인원 1명에게 검진 알림 발송 (NOTIFICATION 저장)
     */
    void sendUnscreenedNotification(Long agencyNo, Long memberNo);

    /**
     * 7일 이상 지연된 미검진 인원에게 검진 알림 일괄 발송 (NOTIFICATION 저장)
     */
    void sendUnscreenedBulkNotification(Long agencyNo);
}
