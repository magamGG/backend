package com.kh.magamGG.domain.manager.service;

import com.kh.magamGG.domain.member.entity.ArtistAssignment;
import com.kh.magamGG.domain.member.entity.Manager;
import com.kh.magamGG.domain.member.repository.ArtistAssignmentRepository;
import com.kh.magamGG.domain.member.repository.ManagerRepository;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtistAssignmentService {

    private final ArtistAssignmentRepository assignmentRepository;
    private final ManagerRepository managerRepository;
    private final MemberRepository memberRepository;
    private final com.kh.magamGG.domain.project.repository.ProjectRepository projectRepository;
    private final com.kh.magamGG.domain.attendance.repository.AttendanceRepository attendanceRepository;

    public void assignArtist(Long managerNo, Long artistMemberNo) {

        if (assignmentRepository.existsByManager_ManagerNoAndArtist_MemberNo(managerNo, artistMemberNo)) {
            throw new IllegalStateException("이미 배정된 작가입니다.");
        }

        Manager manager = managerRepository.findById(managerNo).orElseThrow();
        Member artist = memberRepository.findById(artistMemberNo).orElseThrow();

        ArtistAssignment assignment = ArtistAssignment.builder()
                .manager(manager)
                .artist(artist)
                .build();

        assignmentRepository.save(assignment);
    }

    public List<ArtistAssignment> getAssignedArtists(Long managerNo) {
        return assignmentRepository.findByManagerNo(managerNo);
    }

    public List<ArtistAssignment> getWorkingArtists(Long managerNo) {
        return assignmentRepository.findWorkingArtistsByManagerNo(managerNo);
    }

    public List<com.kh.magamGG.domain.manager.dto.response.AssignedArtistResponse> getWorkingArtistResponses(Long managerNo) {
        List<ArtistAssignment> assignments = assignmentRepository.findWorkingArtistsByManagerNo(managerNo);
        java.time.LocalDate today = java.time.LocalDate.now();

        return assignments.stream()
                .filter(assignment -> {
                    // 오늘 날짜의 마지막 근태 기록 확인 (출근 상태인지)
                    List<com.kh.magamGG.domain.attendance.entity.Attendance> attendances = 
                        attendanceRepository.findTodayLastAttendanceByMemberNo(assignment.getArtist().getMemberNo(), today);
                    
                    // 기록이 없거나, 마지막 기록이 '퇴근'이면 제외
                    if (attendances.isEmpty()) return false;
                    return "출근".equals(attendances.get(0).getAttendanceType());
                })
                .map(assignment -> {
                    Member artist = assignment.getArtist();
                    // 진행 중인 프로젝트 조회
                    List<com.kh.magamGG.domain.project.entity.Project> projects = projectRepository.findActiveProjectsByMemberNo(artist.getMemberNo());
                    String projectName = projects.isEmpty() ? "-" : projects.get(0).getProjectName(); // 첫 번째 프로젝트만 표시

                    // 근태 기록 가져오기 (이미 filter에서 존재 여부 확인됨)
                    List<com.kh.magamGG.domain.attendance.entity.Attendance> attendances = 
                        attendanceRepository.findTodayLastAttendanceByMemberNo(artist.getMemberNo(), today);
                    java.time.LocalDateTime clockInTime = attendances.isEmpty() ? null : attendances.get(0).getAttendanceTime();

                    return new com.kh.magamGG.domain.manager.dto.response.AssignedArtistResponse(
                            artist.getMemberNo(),
                            artist.getMemberName(),
                            artist.getMemberEmail(),
                            artist.getMemberPhone(),
                            artist.getMemberRole(),
                            artist.getMemberStatus(),
                            artist.getMemberProfileImage(),
                            projectName,
                            clockInTime // 출근 시간 포함
                    );
                }).toList();
    }
}
