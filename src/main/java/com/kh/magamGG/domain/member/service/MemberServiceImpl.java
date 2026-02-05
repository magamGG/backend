package com.kh.magamGG.domain.member.service;

import com.kh.magamGG.domain.agency.entity.Agency;
import com.kh.magamGG.domain.agency.repository.AgencyRepository;
import com.kh.magamGG.domain.agency.service.AgencyService;
import com.kh.magamGG.domain.agency.util.AgencyCodeGenerator;
import com.kh.magamGG.domain.member.dto.EmployeeStatisticsResponseDto;
import com.kh.magamGG.domain.member.dto.MemberMyPageResponseDto;
import com.kh.magamGG.domain.member.dto.MemberUpdateRequestDto;
import com.kh.magamGG.domain.member.dto.request.MemberRequest;
import com.kh.magamGG.domain.member.dto.response.MemberDetailResponse;
import com.kh.magamGG.domain.member.dto.response.MemberResponse;
import com.kh.magamGG.domain.member.dto.response.WorkingArtistResponse;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.entity.Manager;
import com.kh.magamGG.domain.member.entity.ArtistAssignment;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.domain.member.repository.ManagerRepository;
import com.kh.magamGG.domain.member.repository.ArtistAssignmentRepository;
import com.kh.magamGG.domain.attendance.entity.Attendance;
import com.kh.magamGG.domain.attendance.repository.AttendanceRepository;
import com.kh.magamGG.domain.health.entity.DailyHealthCheck;
import com.kh.magamGG.domain.health.entity.HealthSurvey;
import com.kh.magamGG.domain.health.repository.DailyHealthCheckRepository;
import com.kh.magamGG.domain.health.repository.HealthSurveyRepository;
import com.kh.magamGG.domain.project.entity.ProjectMember;
import com.kh.magamGG.domain.project.repository.ProjectMemberRepository;
import com.kh.magamGG.domain.notification.service.NotificationService;
import com.kh.magamGG.global.exception.AgencyNotFoundException;
import com.kh.magamGG.global.exception.DuplicateAgencyCodeException;
import com.kh.magamGG.global.exception.DuplicateEmailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final AgencyRepository agencyRepository;
    private final AgencyService agencyService;
    private final PasswordEncoder passwordEncoder;
    private final AgencyCodeGenerator agencyCodeGenerator;
    private final ProjectMemberRepository projectMemberRepository;
    private final DailyHealthCheckRepository dailyHealthCheckRepository;
    private final ManagerRepository managerRepository;
    private final ArtistAssignmentRepository artistAssignmentRepository;
    private final NotificationService notificationService;
    private final AttendanceRepository attendanceRepository;
    private final HealthSurveyRepository healthSurveyRepository;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Override
    @Transactional
    public MemberResponse register(MemberRequest request) {
        // 이메일 중복 체크
        if (memberRepository.existsByMemberEmail(request.getMemberEmail())) {
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
        }

        // 에이전시 처리
        Agency agency = null;
        String agencyCode = null;

        if (request.getAgencyName() != null && !request.getAgencyName().trim().isEmpty()) {
            // 에이전시 관리자인 경우: 새 에이전시 생성
            if (agencyRepository.existsByAgencyName(request.getAgencyName())) {
                throw new DuplicateAgencyCodeException("이미 존재하는 에이전시 이름입니다.");
            }

            agencyCode = agencyCodeGenerator.generateUniqueAgencyCode();
            agency = Agency.builder()
                .agencyName(request.getAgencyName())
                .agencyCode(agencyCode)
                .agencyLeave(15) // 기본 연차 15일
                .build();
            agency = agencyRepository.save(agency);

            // 에이전시 생성 시 해당 에이전시용 HEALTH_SURVEY 4종 생성 (설문기간·주기 디폴트: 15일, 30일)
            createDefaultHealthSurveysForAgency(agency);
        } else if (request.getAgencyCode() != null && !request.getAgencyCode().trim().isEmpty()) {
            // 담당자인 경우: 에이전시 코드로 기존 에이전시 찾기
            agency = agencyService.getAgencyByCode(request.getAgencyCode());
            agencyCode = agency.getAgencyCode();
        } else if (request.getAgencyNo() != null) {
            // 아티스트인 경우: agencyNo로 직접 찾기
            agency = agencyService.getAgency(request.getAgencyNo());
            if (agency != null) {
                agencyCode = agency.getAgencyCode();
            }
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getMemberPassword());

        // Member 엔티티 생성
        Member member = new Member();
        member.setMemberName(request.getMemberName());
        member.setMemberEmail(request.getMemberEmail());
        member.setMemberPassword(encodedPassword);
        member.setMemberPhone(request.getMemberPhone());
        member.setMemberRole(request.getMemberRole());
        member.setMemberStatus("ACTIVE");
        member.setAgency(agency);
        member.setMemberCreatedAt(LocalDateTime.now());
        member.setMemberUpdatedAt(LocalDateTime.now());

        Member savedMember = memberRepository.save(member);

        // 담당자 역할인 경우 MANAGER 테이블에 자동 등록
        if ("담당자".equals(savedMember.getMemberRole())) {
            try {
                // 이미 등록되어 있는지 확인
                Optional<Manager> existingManager = managerRepository.findByMember_MemberNo(savedMember.getMemberNo());
                if (existingManager.isEmpty()) {
                    Manager manager = Manager.builder()
                        .member(savedMember)
                        .build();
                    managerRepository.save(manager);
                    log.info("담당자 자동 등록 완료: memberNo={}, memberName={}", savedMember.getMemberNo(), savedMember.getMemberName());
                } else {
                    log.debug("이미 MANAGER 테이블에 등록된 담당자: memberNo={}", savedMember.getMemberNo());
                }
            } catch (Exception e) {
                log.error("담당자 MANAGER 테이블 등록 실패: memberNo={}, error={}", savedMember.getMemberNo(), e.getMessage(), e);
                // MANAGER 등록 실패해도 회원가입은 성공 처리 (나중에 수동으로 등록 가능)
            }
        }

        return MemberResponse.builder()
            .memberNo(savedMember.getMemberNo())
            .memberName(savedMember.getMemberName())
            .memberEmail(savedMember.getMemberEmail())
            .memberPhone(savedMember.getMemberPhone())
            .memberStatus(savedMember.getMemberStatus())
            .memberRole(savedMember.getMemberRole())
            .memberProfileImage(savedMember.getMemberProfileImage())
            .memberProfileBannerImage(savedMember.getMemberProfileBannerImage())
            .agencyNo(savedMember.getAgency() != null ? savedMember.getAgency().getAgencyNo() : null)
            .agencyCode(agencyCode)
            .memberCreatedAt(savedMember.getMemberCreatedAt())
            .memberUpdatedAt(savedMember.getMemberUpdatedAt())
            .build();
    }

    @Override
    public MemberMyPageResponseDto getMyPageInfo(Long memberNo) {
        Member member = memberRepository.findById(memberNo)
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        Agency agency = member.getAgency();

        return MemberMyPageResponseDto.builder()
            .memberNo(member.getMemberNo())
            .memberName(member.getMemberName())
            .memberEmail(member.getMemberEmail())
            .memberPhone(member.getMemberPhone())
            .memberAddress(member.getMemberAddress())
            .memberProfileImage(member.getMemberProfileImage())
            .memberProfileBannerImage(member.getMemberProfileBannerImage())
            .memberRole(member.getMemberRole())
            .agencyNo(agency != null ? agency.getAgencyNo() : null)
            .agencyName(agency != null ? agency.getAgencyName() : null)
            .agencyCode(agency != null ? agency.getAgencyCode() : null)
            .build();
    }

    @Override
    @Transactional
    public void updateProfile(Long memberNo, MemberUpdateRequestDto requestDto) {
        Member member = memberRepository.findById(memberNo)
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // 이메일은 변경하지 않음 (로그인 ID이므로)
        member.updateProfile(
            requestDto.getMemberName(),
            member.getMemberEmail(), // 기존 이메일 유지
            requestDto.getMemberPhone(),
            requestDto.getMemberAddress()
        );

        // 비밀번호 변경 (제공된 경우에만)
        if (requestDto.getMemberPassword() != null && !requestDto.getMemberPassword().trim().isEmpty()) {
            String encodedPassword = passwordEncoder.encode(requestDto.getMemberPassword());
            member.updatePassword(encodedPassword);
        }

        memberRepository.save(member);

        // 에이전시 대표인 경우 소속(스튜디오) 수정
        if (requestDto.getAgencyName() != null && !requestDto.getAgencyName().isEmpty()) {
            Agency agency = member.getAgency();
            if (agency != null && "에이전시 대표".equals(member.getMemberRole())) {
                agencyService.updateAgencyName(agency.getAgencyNo(), requestDto.getAgencyName());
            }
        }
    }

    @Override
    @Transactional
    public String uploadProfileImage(Long memberNo, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 없습니다.");
        }

        Member member = memberRepository.findById(memberNo)
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        String fileName = saveFile(file);
        member.updateProfileImage(fileName);
        memberRepository.save(member);

        return fileName;
    }

    @Override
    @Transactional
    public String uploadBackgroundImage(Long memberNo, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 없습니다.");
        }

        Member member = memberRepository.findById(memberNo)
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        String fileName = saveFile(file);
        member.updateBackgroundImage(fileName);
        memberRepository.save(member);

        return fileName;
    }

    @Override
    public EmployeeStatisticsResponseDto getEmployeeStatistics(Long agencyNo) {
        List<Object[]> results = memberRepository.countByAgencyNoAndMemberRole(agencyNo);

        List<EmployeeStatisticsResponseDto.RoleCount> roleCounts = results.stream()
            .map(result -> EmployeeStatisticsResponseDto.RoleCount.builder()
                .role((String) result[0])
                .count((Long) result[1])
                .build())
            .collect(Collectors.toList());

        Integer totalCount = roleCounts.stream()
            .mapToInt(count -> count.getCount().intValue())
            .sum();

        return EmployeeStatisticsResponseDto.builder()
            .roleCounts(roleCounts)
            .totalCount(totalCount)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> getMembersByAgencyNo(Long agencyNo) {
        List<Member> members = memberRepository.findByAgency_AgencyNo(agencyNo);
        LocalDate today = LocalDate.now();

        return members.stream()
            .map(member -> {
                String todayWorkStatus = resolveTodayWorkStatus(member.getMemberNo(), today);
                return convertToResponseWithManagerNo(member, null, todayWorkStatus);
            })
            .collect(Collectors.toList());
    }

    /**
     * 오늘 ATTENDANCE 마지막 기록 기준 작업 상태 반환.
     * 출근만 있으면 근무중, 퇴근이 마지막이면 작업 종료, 기록 없으면 작업 시작전.
     */
    private String resolveTodayWorkStatus(Long memberNo, LocalDate today) {
        List<Attendance> list = attendanceRepository.findTodayLastAttendanceByMemberNo(memberNo, today);
        if (list == null || list.isEmpty()) {
            return "작업 시작전";
        }
        String lastType = list.get(0).getAttendanceType();
        if ("출근".equals(lastType)) {
            return "근무중";
        }
        if ("퇴근".equals(lastType)) {
            return "작업 종료";
        }
        return "작업 시작전";
    }

    @Override
    @Transactional
    public void deleteMember(Long memberNo) {
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // 이미 탈퇴한 회원인지 확인
        if ("BLOCKED".equals(member.getMemberStatus())) {
            throw new IllegalArgumentException("이미 탈퇴 처리된 회원입니다.");
        }

        // 실제 삭제 대신 상태를 BLOCKED로 변경
        member.updateStatus("BLOCKED");
        memberRepository.save(member);
    }

    @Override
    public MemberDetailResponse getMemberDetails(Long memberNo) {
        Member member = memberRepository.findById(memberNo)
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // 프로젝트 정보 조회
        List<ProjectMember> projectMembers = projectMemberRepository.findByMember_MemberNo(memberNo);
        List<String> currentProjects = projectMembers.stream()
            .map(pm -> pm.getProject().getProjectName())
            .collect(Collectors.toList());
        List<String> participatedProjects = currentProjects; // 현재는 동일하게 설정

        // 작가의 작품 정보 (현재는 프로젝트와 동일하게 설정)
        List<String> myWorks = currentProjects;
        List<String> serializingWorks = projectMembers.stream()
            .filter(pm -> "연재".equals(pm.getProject().getProjectStatus()))
            .map(pm -> pm.getProject().getProjectName())
            .collect(Collectors.toList());

        // 담당자의 담당 작가 목록 (ARTIST_ASSIGNMENT 테이블에서 조회)
        List<MemberDetailResponse.ManagedArtistInfo> managedArtists = List.of();
        if (("담당자".equals(member.getMemberRole()) || "에이전시 관리자".equals(member.getMemberRole()) || "관리자".equals(member.getMemberRole()))
            && member.getAgency() != null) {
            // MANAGER 테이블에서 해당 멤버의 MANAGER_NO 찾기
            Optional<Manager> managerOpt = managerRepository.findByMember_MemberNo(member.getMemberNo());
            if (managerOpt.isPresent()) {
                List<ArtistAssignment> assignments = artistAssignmentRepository.findByManagerNo(managerOpt.get().getManagerNo());
                managedArtists = assignments.stream()
                    .map(assignment -> {
                        Member artist = assignment.getArtist();
                        return MemberDetailResponse.ManagedArtistInfo.builder()
                            .id(artist.getMemberNo())
                            .name(artist.getMemberName())
                            .role(artist.getMemberRole())
                            .position(artist.getMemberRole())
                            .email(artist.getMemberEmail())
                            .phone(artist.getMemberPhone())
                            .build();
                    })
                    .collect(Collectors.toList());
            }
        }

        // 해당 회원의 가장 최근 데일리 체크 1건만 조회
        MemberDetailResponse.HealthCheckInfo healthCheck = null;
        Optional<DailyHealthCheck> latestOpt = dailyHealthCheckRepository.findFirstByMember_MemberNoOrderByHealthCheckCreatedAtDesc(memberNo);
        if (latestOpt.isPresent()) {
            DailyHealthCheck latest = latestOpt.get();
            healthCheck = MemberDetailResponse.HealthCheckInfo.builder()
                .date(latest.getHealthCheckCreatedAt() != null
                    ? latest.getHealthCheckCreatedAt().toLocalDate().toString()
                    : java.time.LocalDate.now().toString())
                .condition(latest.getHealthCondition() != null ? latest.getHealthCondition() : "보통")
                .sleepHours(latest.getSleepHours() != null ? latest.getSleepHours() : 0)
                .discomfortLevel(latest.getDiscomfortLevel() != null ? latest.getDiscomfortLevel() : 0)
                .memo(latest.getHealthCheckNotes() != null ? latest.getHealthCheckNotes() : "")
                .build();
        }

        return MemberDetailResponse.builder()
            .currentProjects(currentProjects)
            .participatedProjects(participatedProjects)
            .myWorks(myWorks)
            .serializingWorks(serializingWorks)
            .managedArtists(managedArtists)
            .healthCheck(healthCheck)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> getManagersByAgencyNo(Long agencyNo) {
        try {
            log.debug("담당자 목록 조회 시작: agencyNo={}", agencyNo);
            // MANAGER 테이블에서 조회
            List<Manager> managers = managerRepository.findByAgencyNo(agencyNo);
            log.debug("조회된 담당자 수: {}", managers.size());

            if (managers.isEmpty()) {
                return List.of();
            }

            return managers.stream()
                .map(manager -> {
                    try {
                        Member member = manager.getMember();
                        if (member == null) {
                            log.warn("담당자의 멤버 정보가 null입니다: managerNo={}", manager.getManagerNo());
                            return null;
                        }
                        return convertToResponseWithManagerNo(member, manager.getManagerNo());
                    } catch (Exception e) {
                        log.error("담당자 정보 변환 실패: managerNo={}, error={}", manager.getManagerNo(), e.getMessage(), e);
                        return null;
                    }
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("담당자 목록 조회 실패: agencyNo={}, error={}", agencyNo, e.getMessage(), e);
            log.error("스택 트레이스:", e);
            throw new RuntimeException("담당자 목록 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    private MemberResponse convertToResponseWithManagerNo(Member member, Long managerNo) {
        return convertToResponseWithManagerNo(member, managerNo, null);
    }

    private MemberResponse convertToResponseWithManagerNo(Member member, Long managerNo, String todayWorkStatus) {
        MemberResponse.MemberResponseBuilder builder = MemberResponse.builder()
            .memberNo(member.getMemberNo())
            .memberName(member.getMemberName())
            .memberEmail(member.getMemberEmail())
            .memberPhone(member.getMemberPhone())
            .memberStatus(member.getMemberStatus())
            .memberRole(member.getMemberRole())
            .memberProfileImage(member.getMemberProfileImage())
            .memberProfileBannerImage(member.getMemberProfileBannerImage())
            .agencyNo(member.getAgency() != null ? member.getAgency().getAgencyNo() : null)
            .agencyCode(member.getAgency() != null ? member.getAgency().getAgencyCode() : null)
            .managerNo(managerNo)
            .memberCreatedAt(member.getMemberCreatedAt())
            .memberUpdatedAt(member.getMemberUpdatedAt());
        if (todayWorkStatus != null) {
            builder.todayWorkStatus(todayWorkStatus);
        }
        return builder.build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> getArtistsByAgencyNo(Long agencyNo) {
        try {
            List<Member> artists = memberRepository.findArtistsByAgencyNo(agencyNo);

            // 각 작가의 배정된 담당자 정보 조회
            return artists.stream()
                .map(artist -> {
                    // ARTIST_ASSIGNMENT에서 배정 조회하여 managerNo 설정
                    Optional<ArtistAssignment> assignment = artistAssignmentRepository.findByArtistMemberNo(artist.getMemberNo());
                    Long managerNo = assignment.map(a -> a.getManager().getManagerNo()).orElse(null);
                    return convertToResponseWithManagerNo(artist, managerNo);
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("작가 목록 조회 실패: agencyNo={}, error={}", agencyNo, e.getMessage(), e);
            throw new RuntimeException("작가 목록 조회 중 오류가 발생했습니다.", e);
        }
    }


    @Override
    @Transactional
    public void assignArtistToManager(Long artistNo, Long managerNo) {
        log.info("작가 배정 시작: artistNo={}, managerNo={} (MANAGER 테이블의 MANAGER_NO)", artistNo, managerNo);

        Member artist = memberRepository.findById(artistNo)
            .orElseThrow(() -> new IllegalArgumentException("작가를 찾을 수 없습니다."));

        // managerNo는 MANAGER 테이블의 MANAGER_NO이므로 Manager 엔티티 조회
        Manager manager = managerRepository.findById(managerNo)
            .orElseThrow(() -> new IllegalArgumentException("담당자를 찾을 수 없습니다."));

        log.debug("작가 정보: memberNo={}, name={}, role={}", artist.getMemberNo(), artist.getMemberName(), artist.getMemberRole());
        log.debug("담당자 정보: managerNo={}, memberNo={}, name={}, role={}",
            manager.getManagerNo(), manager.getMember().getMemberNo(),
            manager.getMember().getMemberName(), manager.getMember().getMemberRole());

        // 작가 역할 확인
        if (!artist.getMemberRole().equals("웹툰 작가") && !artist.getMemberRole().equals("웹소설 작가")) {
            throw new IllegalArgumentException("작가만 배정할 수 있습니다.");
        }

        // 담당자 역할 확인
        if (!manager.getMember().getMemberRole().equals("담당자")) {
            throw new IllegalArgumentException("담당자에게만 배정할 수 있습니다.");
        }

        // 같은 에이전시인지 확인
        if (artist.getAgency() == null || manager.getMember().getAgency() == null ||
            !artist.getAgency().getAgencyNo().equals(manager.getMember().getAgency().getAgencyNo())) {
            throw new IllegalArgumentException("같은 에이전시 소속이 아닙니다.");
        }

        // 기존 배정이 있으면 삭제
        Optional<ArtistAssignment> existingAssignment = artistAssignmentRepository.findByArtistMemberNo(artistNo);
        if (existingAssignment.isPresent()) {
            log.info("기존 배정 삭제: artistNo={}, 기존 managerNo={}", artistNo, existingAssignment.get().getManager().getManagerNo());
            artistAssignmentRepository.delete(existingAssignment.get());
        }

        // 새 배정 생성 - ARTIST_ASSIGNMENT 테이블에 등록
        // MANAGER_NO 컬럼에는 선택된 담당자의 MANAGER_NO가 저장됨
        ArtistAssignment assignment = ArtistAssignment.builder()
            .manager(manager)  // MANAGER 테이블의 MANAGER_NO가 ARTIST_ASSIGNMENT.MANAGER_NO에 저장됨
            .artist(artist)    // MEMBER 테이블의 MEMBER_NO가 ARTIST_ASSIGNMENT.ARTIST_MEMBER_NO에 저장됨
            .build();

        ArtistAssignment savedAssignment = artistAssignmentRepository.save(assignment);
        log.info("작가 배정 완료: ARTIST_ASSIGNMENT_NO={}, ARTIST_MEMBER_NO={}, MANAGER_NO={}",
            savedAssignment.getArtistAssignmentNo(),
            savedAssignment.getArtist().getMemberNo(),
            savedAssignment.getManager().getManagerNo());

        // 알림 생성 (LAZY 로딩을 위해 트랜잭션 내에서 접근)
        Member managerMember = manager.getMember();
        String notificationType = "ASSIGNMENT";

        createNotificationSafely(
            managerMember.getMemberNo(),
            "작가 배정 알림",
            artist.getMemberName() + "님이 배정되었습니다",
            notificationType
        );

        createNotificationSafely(
            artist.getMemberNo(),
            "담당자 배정 알림",
            managerMember.getMemberName() + " 담당자가 배정되었습니다",
            notificationType
        );
    }

    @Override
    @Transactional
    public void unassignArtistFromManager(Long artistNo) {
        log.info("작가 배정 해제 시작: artistNo={}", artistNo);

        Optional<ArtistAssignment> assignment = artistAssignmentRepository.findByArtistMemberNo(artistNo);

        if (assignment.isEmpty()) {
            log.warn("배정된 담당자가 없음: artistNo={}", artistNo);
            throw new IllegalArgumentException("배정된 담당자가 없습니다.");
        }

        ArtistAssignment assignmentToDelete = assignment.get();

        // 삭제 전에 담당자와 작가 정보 가져오기 (LAZY 로딩을 위해 트랜잭션 내에서 접근)
        Member artist = assignmentToDelete.getArtist();
        Manager manager = assignmentToDelete.getManager();
        Member managerMember = manager.getMember();

        log.info("배정 해제: ARTIST_ASSIGNMENT_NO={}, ARTIST_MEMBER_NO={}, MANAGER_NO={}",
            assignmentToDelete.getArtistAssignmentNo(),
            artist.getMemberNo(),
            manager.getManagerNo());

        // 배정 해제 전에 알림 생성
        String notificationType = "ASSIGNMENT";

        createNotificationSafely(
            managerMember.getMemberNo(),
            "작가 배정 해제 알림",
            artist.getMemberName() + "님의 배정이 해제되었습니다",
            notificationType
        );

        createNotificationSafely(
            artist.getMemberNo(),
            "담당자 배정 해제 알림",
            managerMember.getMemberName() + " 담당자의 배정이 해제되었습니다",
            notificationType
        );

        // 배정 해제 실행
        artistAssignmentRepository.delete(assignmentToDelete);
        log.info("작가 배정 해제 완료: artistNo={}", artistNo);
    }

    private static final List<String> ARTIST_ROLES = List.of("웹툰 작가", "웹소설 작가");

    /**
     * 담당자(MANAGER_NO)에게 배정된 작가만 반환.
     * MANAGER 테이블의 manager_no로 ARTIST_ASSIGNMENT에서 해당 manager_no와 관계 있는 member_no를 조회하고,
     * 그 회원의 MEMBER_ROLE이 '웹툰 작가', '웹소설 작가'인 경우만 포함.
     */
    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> getArtistsByManagerNo(Long managerNo) {
        try {
            log.debug("담당자별 작가 목록 조회 시작: managerNo={}", managerNo);
            List<ArtistAssignment> assignments = artistAssignmentRepository.findByManagerNo(managerNo);
            log.debug("조회된 배정 수: {}", assignments.size());

            return assignments.stream()
                .map(ArtistAssignment::getArtist)
                .filter(artist -> artist != null && artist.getMemberRole() != null
                    && ARTIST_ROLES.contains(artist.getMemberRole().trim()))
                .map(artist -> convertToResponseWithManagerNo(artist, managerNo))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("담당자별 작가 목록 조회 실패: managerNo={}, error={}", managerNo, e.getMessage(), e);
            throw new RuntimeException("담당자별 작가 목록 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 현재 로그인한 담당자(member)의 manager_no를 MANAGER 테이블에서 찾고,
     * ARTIST_ASSIGNMENT에서 그 manager_no와 관계 있는 member_no 중 MEMBER_ROLE이 '웹툰 작가', '웹소설 작가'인 회원만 반환.
     * 배정된 작가가 없으면 빈 목록 반환.
     */
    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> getAssignedArtistsByMemberNo(Long memberNo) {
        return managerRepository.findByMember_MemberNo(memberNo)
            .map(manager -> getArtistsByManagerNo(manager.getManagerNo()))
            .orElse(List.of());
    }

    /**
     * 담당자(manager_no)에게 배정된 작가(ARTIST_ASSIGNMENT.ARTIST_MEMBER_NO) 중,
     * 오늘 ATTENDANCE 마지막 이력이 '출근'인 사람만 반환. (로그인한 담당자 본인 출근 여부는 조회하지 않음)
     */
    @Override
    @Transactional(readOnly = true)
    public List<WorkingArtistResponse> getWorkingArtistsByManagerNo(Long managerNo) {
        LocalDate today = LocalDate.now();
        List<ArtistAssignment> assignments = artistAssignmentRepository.findByManagerNo(managerNo);
        log.info("현재 작업중인 작가 조회: managerNo={}, 오늘={}, 배정 수={}", managerNo, today, assignments.size());
        List<WorkingArtistResponse> result = assignments.stream()
            .map(ArtistAssignment::getArtist)  // ARTIST_MEMBER_NO 해당 회원
            .map(artist -> {
                List<Attendance> todayAttendances = attendanceRepository.findTodayLastAttendanceByMemberNo(artist.getMemberNo(), today);
                if (todayAttendances.isEmpty()) {
                    log.trace("작가 memberNo={} 오늘 출퇴근 기록 없음", artist.getMemberNo());
                    return null;
                }
                Attendance last = todayAttendances.get(0);
                if (!"출근".equals(last.getAttendanceType())) {
                    log.trace("작가 memberNo={} 마지막 타입={} (출근만 포함)", artist.getMemberNo(), last.getAttendanceType());
                    return null;
                }
                return WorkingArtistResponse.builder()
                    .memberNo(artist.getMemberNo())
                    .memberName(artist.getMemberName())
                    .clockInTime(last.getAttendanceTime())
                    .build();
            })
            .filter(r -> r != null)
            .collect(Collectors.toList());
        log.info("현재 작업중인 작가 조회 완료: managerNo={}, 반환 수={}", managerNo, result.size());
        return result;
    }

    @Override
    @Transactional
    public void removeFromAgency(Long memberNo) {
        log.info("에이전시에서 회원 제거 시작: memberNo={}", memberNo);

        Member member = memberRepository.findById(memberNo)
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        if (member.getAgency() == null) {
            log.warn("이미 에이전시에 소속되지 않은 회원: memberNo={}", memberNo);
            throw new IllegalArgumentException("이미 에이전시에 소속되지 않은 회원입니다.");
        }

        // agencyNo를 null로 설정
        member.setAgency(null);
        memberRepository.save(member);

        log.info("에이전시에서 회원 제거 완료: memberNo={}", memberNo);
    }

    private MemberResponse convertToResponse(Member member) {
        return convertToResponseWithManagerNo(member, null, null);
    }

    /**
     * 에이전시 생성 시 해당 에이전시에 연결된 HEALTH_SURVEY 1건 생성.
     * HEALTH_SURVEY_PERIOD 15일, HEALTH_SURVEY_CYCLE 30일 디폴트 적용.
     */
    private void createDefaultHealthSurveysForAgency(Agency agency) {
        int defaultPeriod = 15;
        int defaultCycle = 30;

        LocalDateTime now = LocalDateTime.now();
        HealthSurvey survey = HealthSurvey.builder()
            .agency(agency)
            .healthSurveyPeriod(defaultPeriod)
            .healthSurveyCycle(defaultCycle)
            .healthSurveyCreatedAt(now)
            .healthSurveyUpdatedAt(now)
            .build();
        healthSurveyRepository.save(survey);
        log.info("에이전시 HEALTH_SURVEY 생성 완료: agencyNo={}, healthSurveyNo={}, period={}, cycle={}",
            agency.getAgencyNo(), survey.getHealthSurveyNo(), defaultPeriod, defaultCycle);
    }

    /**
     * 알림 생성 헬퍼 메서드 (실패해도 트랜잭션 롤백 방지)
     */
    private void createNotificationSafely(Long memberNo, String name, String text, String type) {
        try {
            notificationService.createNotification(memberNo, name, text, type);
            log.debug("알림 생성 완료: memberNo={}, name={}", memberNo, name);
        } catch (Exception e) {
            log.warn("알림 생성 실패 (무시): memberNo={}, name={}, error={}", memberNo, name, e.getMessage());
        }
    }

    private String saveFile(MultipartFile file) {
        try {
            // 업로드 디렉토리 생성
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 파일명 생성 (UUID + 원본 파일명)
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
            String fileName = UUID.randomUUID().toString() + extension;

            // 파일 저장
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            return fileName;
        } catch (IOException e) {
            log.error("파일 저장 실패: {}", e.getMessage());
            throw new RuntimeException("파일 저장에 실패했습니다.", e);
        }
    }

    private void deleteFile(String fileName) {
        try {
            Path filePath = Paths.get(uploadDir, fileName);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            log.error("파일 삭제 실패: {}", e.getMessage());
        }
    }
}
