package com.kh.magamGG.domain.agency.service;

import com.kh.magamGG.domain.agency.entity.Agency;

public interface AgencyService {
	
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


