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
import com.kh.magamGG.global.exception.NewRequestNotFoundException;
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
        // 1. 에이전시 코드로 에이전시 조회
        Agency agency = agencyRepository.findByAgencyCode(request.getAgencyCode())
                .orElseThrow(() -> new AgencyNotFoundException("존재하지 않는 에이전시 코드입니다."));
        
        // 2. 멤버 조회
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 멤버입니다."));
        
        // 3. 이미 해당 에이전시에 가입 요청이 있는지 확인 (대기 상태인 경우)
        List<NewRequest> existingRequests = newRequestRepository.findByMember_MemberNo(memberNo);
        boolean hasPendingRequest = existingRequests.stream()
                .anyMatch(nr -> nr.getAgency().getAgencyNo().equals(agency.getAgencyNo())
                        && "대기".equals(nr.getNewRequestStatus()));
        
        if (hasPendingRequest) {
            throw new IllegalArgumentException("이미 해당 에이전시에 가입 요청이 진행중입니다.");
        }
        
        // 4. NewRequest 생성 및 저장
        NewRequest newRequest = NewRequest.builder()
                .agency(agency)
                .member(member)
                .newRequestDate(LocalDateTime.now())
                .newRequestStatus("대기") // Git 버전과 호환: 한글 상태 코드 사용
                .build();
        
        newRequest = newRequestRepository.save(newRequest);
        log.info("에이전시 가입 요청 생성: 멤버 {} -> 에이전시 {} (요청번호: {})",
                member.getMemberName(), agency.getAgencyName(), newRequest.getNewRequestNo());
        
        // 5. 응답 생성
        return convertToResponse(newRequest);
    }
    
    @Override
    public List<JoinRequestResponse> getJoinRequests(Long agencyNo) {
        // 에이전시 존재 확인
        Agency agency = agencyRepository.findById(agencyNo)
                .orElseThrow(() -> new AgencyNotFoundException("존재하지 않는 에이전시입니다."));
        
        // 해당 에이전시의 모든 가입 요청 조회 (최신순, Member와 Agency 함께 로드)
        List<NewRequest> requests = newRequestRepository.findByAgency_AgencyNoOrderByNewRequestDateDesc(agencyNo);
        
        return requests.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public JoinRequestResponse approveJoinRequest(Long newRequestNo) {
        // 가입 요청 조회 (Member와 Agency를 함께 로드)
        NewRequest newRequest = newRequestRepository.findByIdWithMemberAndAgency(newRequestNo)
                .orElseThrow(() -> new NewRequestNotFoundException("존재하지 않는 가입 요청입니다."));
        
        // 이미 처리된 요청인지 확인
        if (!"대기".equals(newRequest.getNewRequestStatus())) {
            throw new IllegalStateException("이미 처리된 요청입니다.");
        }
        
        // 멤버 조회
        Member member = newRequest.getMember();
        if (member == null || member.getMemberNo() == null) {
            throw new MemberNotFoundException("해당 멤버가 없습니다.");
        }
        
        // 멤버가 실제로 DB에 존재하는지 확인
        Member existingMember = memberRepository.findById(member.getMemberNo())
                .orElseThrow(() -> new MemberNotFoundException("해당 멤버가 없습니다."));
        
        // 기존 멤버의 에이전시 정보 업데이트
        existingMember.setAgency(newRequest.getAgency()); // 에이전시 연결
        existingMember.setMemberStatus("ACTIVE"); // 상태를 활성으로 변경
        existingMember.setMemberUpdatedAt(LocalDateTime.now()); // 업데이트 시간 설정
        
        memberRepository.save(existingMember);
        
        // 요청 상태를 승인으로 변경 (setter 사용)
        newRequest.setNewRequestStatus("승인");
        newRequest = newRequestRepository.save(newRequest);
        
        log.info("에이전시 가입 요청 승인: 요청번호 {}, 멤버 {} -> 에이전시 {}", 
                newRequestNo, existingMember.getMemberName(), newRequest.getAgency().getAgencyName());
        
        return convertToResponse(newRequest);
    }
    
    @Override
    @Transactional
    public JoinRequestResponse rejectJoinRequest(Long newRequestNo, String rejectionReason) {
        // 가입 요청 조회 (Member와 Agency를 함께 로드)
        NewRequest newRequest = newRequestRepository.findByIdWithMemberAndAgency(newRequestNo)
                .orElseThrow(() -> new NewRequestNotFoundException("존재하지 않는 가입 요청입니다."));
        
        // 이미 처리된 요청인지 확인
        if (!"대기".equals(newRequest.getNewRequestStatus())) {
            throw new IllegalStateException("이미 처리된 요청입니다.");
        }
        
        String memberName = newRequest.getMember().getMemberName();
        
        // 요청 상태를 거절로 변경 (setter 사용)
        newRequest.setNewRequestStatus("거절");
        newRequest = newRequestRepository.save(newRequest);
        
        log.info("에이전시 가입 요청 거절: 요청번호 {}, 멤버 {}, 사유: {}", 
                newRequestNo, memberName, rejectionReason);
        
        return convertToResponse(newRequest);
    }
    
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
    
    private JoinRequestResponse convertToResponse(NewRequest newRequest) {
        Member member = newRequest.getMember();
        Agency agency = newRequest.getAgency();
        
        if (member == null || agency == null) {
            throw new IllegalStateException("멤버 또는 에이전시 정보가 없습니다.");
        }
        
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
                .agencyName(agency.getAgencyName())
                .status(newRequest.getNewRequestStatus()) // 프론트엔드 호환
                .requestDate(newRequest.getNewRequestDate()) // 프론트엔드 호환
                .build();
    }
}
