package com.kh.magamGG.domain.agency.service;

import com.kh.magamGG.domain.agency.dto.request.JoinRequestRequest;
import com.kh.magamGG.domain.agency.dto.response.JoinRequestResponse;
import com.kh.magamGG.domain.agency.entity.Agency;
import com.kh.magamGG.domain.agency.repository.AgencyRepository;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.entity.NewRequest;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.domain.member.repository.NewRequestRepository;
import com.kh.magamGG.global.exception.AgencyNotFoundException;
import com.kh.magamGG.global.exception.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AgencyServiceImpl implements AgencyService {
    
    private final AgencyRepository agencyRepository;
    private final MemberRepository memberRepository;
    private final NewRequestRepository newRequestRepository;
    
    @Override
    @Transactional
    public JoinRequestResponse createJoinRequest(JoinRequestRequest request, Long memberNo) {
        // 에이전시 코드로 에이전시 조회
        Agency agency = agencyRepository.findByAgencyCode(request.getAgencyCode())
                .orElseThrow(() -> new AgencyNotFoundException("존재하지 않는 에이전시 코드입니다."));
        
        // 회원 조회
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다."));
        
        // 이미 해당 에이전시에 가입 요청이 있는지 확인 (대기 상태인 경우)
        List<NewRequest> existingRequests = newRequestRepository.findByMember_MemberNo(memberNo);
        boolean hasPendingRequest = existingRequests.stream()
                .anyMatch(nr -> nr.getAgency().getAgencyNo().equals(agency.getAgencyNo()) 
                        && "대기".equals(nr.getNewRequestStatus()));
        
        if (hasPendingRequest) {
            throw new IllegalArgumentException("이미 해당 에이전시에 가입 요청이 대기 중입니다.");
        }
        
        // NEW_REQUEST 생성
        NewRequest newRequest = NewRequest.builder()
                .agency(agency)
                .member(member)
                .newRequestDate(LocalDateTime.now())
                .newRequestStatus("대기")
                .build();
        
        newRequest = newRequestRepository.save(newRequest);
        log.info("에이전시 가입 요청 생성: 회원 {} -> 에이전시 {} (요청번호: {})", 
                member.getMemberName(), agency.getAgencyName(), newRequest.getNewRequestNo());
        
        return JoinRequestResponse.builder()
                .newRequestNo(newRequest.getNewRequestNo())
                .agencyNo(agency.getAgencyNo())
                .memberNo(member.getMemberNo())
                .memberName(member.getMemberName())
                .memberEmail(member.getMemberEmail())
                .memberPhone(member.getMemberPhone())
                .memberRole(member.getMemberRole())
                .newRequestDate(newRequest.getNewRequestDate())
                .newRequestStatus(newRequest.getNewRequestStatus())
                .build();
    }
    
    @Override
    public List<JoinRequestResponse> getJoinRequests(Long agencyNo) {
        // 에이전시 존재 확인
        Agency agency = agencyRepository.findById(agencyNo)
                .orElseThrow(() -> new AgencyNotFoundException("존재하지 않는 에이전시입니다."));
        
        // 해당 에이전시의 모든 가입 요청 조회 (최신순)
        List<NewRequest> requests = newRequestRepository.findByAgency_AgencyNoOrderByNewRequestDateDesc(agencyNo);
        
        return requests.stream()
                .map(nr -> JoinRequestResponse.builder()
                        .newRequestNo(nr.getNewRequestNo())
                        .agencyNo(nr.getAgency().getAgencyNo())
                        .memberNo(nr.getMember().getMemberNo())
                        .memberName(nr.getMember().getMemberName())
                        .memberEmail(nr.getMember().getMemberEmail())
                        .memberPhone(nr.getMember().getMemberPhone())
                        .memberRole(nr.getMember().getMemberRole())
                        .newRequestDate(nr.getNewRequestDate())
                        .newRequestStatus(nr.getNewRequestStatus())
                        .build())
                .collect(Collectors.toList());
    }
}
