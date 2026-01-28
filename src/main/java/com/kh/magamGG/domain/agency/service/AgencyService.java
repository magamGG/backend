package com.kh.magamGG.domain.agency.service;

import com.kh.magamGG.domain.agency.dto.request.JoinRequestRequest;
import com.kh.magamGG.domain.agency.dto.response.JoinRequestResponse;

import java.util.List;
import com.kh.magamGG.domain.agency.entity.Agency;

public interface AgencyService {
    JoinRequestResponse createJoinRequest(JoinRequestRequest request, Long memberNo);
    List<JoinRequestResponse> getJoinRequests(Long agencyNo);
    JoinRequestResponse approveJoinRequest(Long newRequestNo);
    JoinRequestResponse rejectJoinRequest(Long newRequestNo, String rejectionReason);

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
