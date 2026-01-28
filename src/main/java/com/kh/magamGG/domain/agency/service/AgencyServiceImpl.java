package com.kh.magamGG.domain.agency.service;

import com.kh.magamGG.domain.agency.entity.Agency;
import com.kh.magamGG.domain.agency.repository.AgencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgencyServiceImpl implements AgencyService {
	
	private final AgencyRepository agencyRepository;
	
	@Override
	public Agency getAgency(Long agencyNo) {
		return agencyRepository.findById(agencyNo)
			.orElseThrow(() -> new IllegalArgumentException("에이전시를 찾을 수 없습니다."));
	}
	
	@Override
	public Agency getAgencyByCode(String agencyCode) {
		return agencyRepository.findByAgencyCode(agencyCode)
			.orElseThrow(() -> new IllegalArgumentException("에이전시를 찾을 수 없습니다."));
	}
	
	@Override
	@Transactional
	public void updateAgencyName(Long agencyNo, String agencyName) {
		Agency agency = agencyRepository.findById(agencyNo)
			.orElseThrow(() -> new IllegalArgumentException("에이전시를 찾을 수 없습니다."));
		
		agency.updateAgencyName(agencyName);
		agencyRepository.save(agency);
	}
}
