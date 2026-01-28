package com.kh.magamGG.domain.agency.service;

import com.kh.magamGG.domain.agency.dto.request.JoinRequestRequest;
import com.kh.magamGG.domain.agency.dto.response.JoinRequestResponse;
import com.kh.magamGG.domain.agency.entity.Agency;

import java.util.List;

public interface AgencyService {
    // Git 버전과 호환되는 메서드 시그니처
    JoinRequestResponse createJoinRequest(JoinRequestRequest request, Long memberNo);
    List<JoinRequestResponse> getJoinRequests(Long agencyNo);
    JoinRequestResponse approveJoinRequest(Long newRequestNo);
    JoinRequestResponse rejectJoinRequest(Long newRequestNo, String rejectionReason);
    
    // Git 버전의 추가 메서드들
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
}
