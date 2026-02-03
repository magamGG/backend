package com.kh.magamGG.domain.agency.service;

import com.kh.magamGG.domain.agency.dto.request.JoinRequestRequest;
import com.kh.magamGG.domain.agency.dto.response.JoinRequestResponse;
import com.kh.magamGG.domain.agency.entity.Agency;
import com.kh.magamGG.domain.agency.repository.AgencyRepository;
import com.kh.magamGG.domain.attendance.entity.LeaveBalance;
import com.kh.magamGG.domain.attendance.repository.LeaveBalanceRepository;
import com.kh.magamGG.domain.member.entity.Manager;
import com.kh.magamGG.domain.member.repository.ManagerRepository;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.entity.NewRequest;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.domain.member.repository.NewRequestRepository;
import com.kh.magamGG.domain.notification.service.NotificationService;
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
    private final NotificationService notificationService;
    private final ManagerRepository managerRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;

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

        // 에이전시 담당자에게 알림 발송
        String notificationName = "가입 요청";
        String notificationText = String.format("%s님(%s)이 에이전시 가입을 요청했습니다.", 
                member.getMemberName(),
                member.getMemberRole());
        
        notificationService.notifyAgencyManagers(
                agency.getAgencyNo(),
                notificationName,
                notificationText,
                "JOIN_REQ"
        );

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

    @Override
    @Transactional
    public JoinRequestResponse approveJoinRequest(Long newRequestNo) {
        // 가입 요청 조회
        NewRequest newRequest = newRequestRepository.findById(newRequestNo)
                .orElseThrow(() -> new NewRequestNotFoundException("존재하지 않는 가입 요청입니다."));

        // 상태 확인 (대기 상태만 승인 가능)
        if (!"대기".equals(newRequest.getNewRequestStatus())) {
            throw new IllegalArgumentException("이미 처리된 요청입니다. (현재 상태: " + newRequest.getNewRequestStatus() + ")");
        }

        Long memberNo = newRequest.getMember().getMemberNo();
        Long agencyNo = newRequest.getAgency().getAgencyNo();
        String memberName = newRequest.getMember().getMemberName();
        String agencyName = newRequest.getAgency().getAgencyName();

        // 1. NEW_REQUEST 상태를 "승인"으로 변경 (JPA)
        newRequest.setNewRequestStatus("승인");
        newRequestRepository.save(newRequest);
        log.info("NEW_REQUEST 상태 업데이트 완료: {} -> 승인", newRequestNo);

        // 2. MEMBER의 AGENCY_NO를 업데이트 (JPA)
        Member memberToUpdate = memberRepository.findById(memberNo)
                .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다."));
        Agency agencyToAssign = agencyRepository.findById(agencyNo)
                .orElseThrow(() -> new AgencyNotFoundException("에이전시를 찾을 수 없습니다."));
        memberToUpdate.setAgency(agencyToAssign);
        memberRepository.save(memberToUpdate);
        log.info("MEMBER AGENCY_NO 업데이트 완료: 회원 {} -> 에이전시 {}", memberNo, agencyNo);

        // 3. LEAVE_BALANCE: 같은 memberNo + 연도 있으면 덮어쓰기, 없으면 신규 생성
        int totalDays = newRequest.getAgency().getAgencyLeave() != null
                ? newRequest.getAgency().getAgencyLeave() : 15;
        String currentYear = String.valueOf(java.time.Year.now().getValue());
        Member memberForBalance = memberRepository.findById(memberNo)
                .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다."));
        LeaveBalance leaveBalance = leaveBalanceRepository
                .findByMember_MemberNoAndLeaveBalanceYear(memberNo, currentYear)
                .orElse(null);
        if (leaveBalance != null) {
            leaveBalance.setLeaveBalanceTotalDays(totalDays);
            leaveBalance.setLeaveBalanceUsedDays(0);
            leaveBalance.setLeaveBalanceRemainDays((double) totalDays);
            leaveBalance.setLeaveBalanceUpdatedAt(LocalDateTime.now());
            leaveBalanceRepository.save(leaveBalance);
            log.info("LEAVE_BALANCE 덮어쓰기 완료: 회원번호 {}, 총연차일 {}, 연도 {}", memberNo, totalDays, currentYear);
        } else {
            leaveBalance = new LeaveBalance();
            leaveBalance.setMember(memberForBalance);
            leaveBalance.setLeaveType("ANNUAL");
            leaveBalance.setLeaveBalanceTotalDays(totalDays);
            leaveBalance.setLeaveBalanceUsedDays(0);
            leaveBalance.setLeaveBalanceRemainDays((double) totalDays);
            leaveBalance.setLeaveBalanceYear(currentYear);
            leaveBalance.setLeaveBalanceUpdatedAt(LocalDateTime.now());
            leaveBalanceRepository.save(leaveBalance);
            log.info("LEAVE_BALANCE 초기 데이터 생성 완료: 회원번호 {}, 총연차일 {}, 연도 {}", memberNo, totalDays, currentYear);
        }

        // 4. 담당자인 경우 MANAGER 테이블에 등록 (작가 배정 기능을 위해)
        String memberRole = newRequest.getMember().getMemberRole();
        if ("담당자".equals(memberRole)) {
            // 이미 Manager로 등록되어 있는지 확인
            boolean alreadyManager = managerRepository.findByMember_MemberNo(memberNo).isPresent();
            if (!alreadyManager) {
                Member memberEntity = memberRepository.findById(memberNo)
                        .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다."));
                
                Manager manager = Manager.builder()
                        .member(memberEntity)
                        .build();
                managerRepository.save(manager);
                log.info("MANAGER 테이블에 담당자 등록 완료: 회원번호 {}, 회원명 {}", memberNo, memberName);
            }
        }

        log.info("에이전시 가입 요청 승인 완료: 요청번호 {}, 회원 {} -> 에이전시 {} 소속으로 변경",
                newRequestNo, memberName, agencyName);

        // 업데이트된 엔티티 다시 조회해서 반환
        newRequest = newRequestRepository.findById(newRequestNo)
                .orElseThrow(() -> new NewRequestNotFoundException("업데이트 후 요청을 찾을 수 없습니다."));
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new MemberNotFoundException("업데이트 후 회원을 찾을 수 없습니다."));

        return JoinRequestResponse.builder()
                .newRequestNo(newRequest.getNewRequestNo())
                .agencyNo(agencyNo)
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
    @Transactional
    public JoinRequestResponse rejectJoinRequest(Long newRequestNo, String rejectionReason) {
        // 가입 요청 조회
        NewRequest newRequest = newRequestRepository.findById(newRequestNo)
                .orElseThrow(() -> new NewRequestNotFoundException("존재하지 않는 가입 요청입니다."));

        // 상태 확인 (대기 상태만 거절 가능)
        if (!"대기".equals(newRequest.getNewRequestStatus())) {
            throw new IllegalArgumentException("이미 처리된 요청입니다. (현재 상태: " + newRequest.getNewRequestStatus() + ")");
        }

        String memberName = newRequest.getMember().getMemberName();

        // 가입 요청 상태를 "거절"로 변경 (JPA)
        newRequest.setNewRequestStatus("거절");
        newRequestRepository.save(newRequest);
        
        log.info("에이전시 가입 요청 거절: 요청번호 {}, 회원 {}, 사유: {}",
                newRequestNo, memberName, rejectionReason);

        // 업데이트된 엔티티 다시 조회
        newRequest = newRequestRepository.findById(newRequestNo)
                .orElseThrow(() -> new NewRequestNotFoundException("업데이트 후 요청을 찾을 수 없습니다."));

        return JoinRequestResponse.builder()
                .newRequestNo(newRequest.getNewRequestNo())
                .agencyNo(newRequest.getAgency().getAgencyNo())
                .memberNo(newRequest.getMember().getMemberNo())
                .memberName(newRequest.getMember().getMemberName())
                .memberEmail(newRequest.getMember().getMemberEmail())
                .memberPhone(newRequest.getMember().getMemberPhone())
                .memberRole(newRequest.getMember().getMemberRole())
                .newRequestDate(newRequest.getNewRequestDate())
                .newRequestStatus(newRequest.getNewRequestStatus())
                .build();
    }

    /**
     * 에이전시 조회 (없으면 AgencyNotFoundException) — 연차 관리 등 공통 사용
     */
    private Agency findAgencyOrThrow(Long agencyNo) {
        return agencyRepository.findById(agencyNo)
                .orElseThrow(() -> new AgencyNotFoundException("존재하지 않는 에이전시입니다."));
    }

    @Override
    public Agency getAgency(Long agencyNo) {
        return findAgencyOrThrow(agencyNo);
    }

    @Override
    public Agency getAgencyByCode(String agencyCode) {
        return agencyRepository.findByAgencyCode(agencyCode)
                .orElseThrow(() -> new AgencyNotFoundException("존재하지 않는 에이전시 코드입니다."));
    }

    @Override
    @Transactional
    public void updateAgencyName(Long agencyNo, String agencyName) {
        Agency agency = findAgencyOrThrow(agencyNo);
        agency.updateAgencyName(agencyName);
        agencyRepository.save(agency);
    }

    @Override
    @Transactional
    public void updateAgencyLeave(Long agencyNo, Integer agencyLeave) {
        if (agencyLeave == null || agencyLeave < 0) {
            throw new IllegalArgumentException("기본 연차는 0 이상이어야 합니다.");
        }
        Agency agency = findAgencyOrThrow(agencyNo);
        agency.updateAgencyLeave(agencyLeave);
        agencyRepository.save(agency);

        // 해당 에이전시 소속 회원들의 당해 연도 LeaveBalance.leaveBalanceTotalDays 동일 값으로 갱신
        String currentYear = String.valueOf(java.time.Year.now().getValue());
        List<Member> members = memberRepository.findByAgency_AgencyNo(agencyNo);
        for (Member member : members) {
            leaveBalanceRepository.findByMember_MemberNoAndLeaveBalanceYear(member.getMemberNo(), currentYear)
            .ifPresent(balance -> {
                balance.setLeaveBalanceTotalDays(agencyLeave);
                int used = balance.getLeaveBalanceUsedDays() != null ? balance.getLeaveBalanceUsedDays() : 0;
                double newRemain = agencyLeave - used;
                balance.setLeaveBalanceRemainDays(newRemain >= 0 ? newRemain : 0.0);
                balance.setLeaveBalanceUpdatedAt(LocalDateTime.now());
                leaveBalanceRepository.save(balance);
                log.info("LeaveBalance 갱신: 회원번호={}, totalDays={}, remainDays={}", member.getMemberNo(), agencyLeave, balance.getLeaveBalanceRemainDays());
            });
        }
    }
}
