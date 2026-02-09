package com.kh.magamGG.domain.attendance.entity;

import com.kh.magamGG.domain.project.entity.Project;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "PROJECT_LEAVE_REQUEST", 
       uniqueConstraints = @UniqueConstraint(name = "UK_ATTENDANCE_REQUEST_PROJECT", 
                                            columnNames = {"ATTENDANCE_REQUEST_NO"}))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectLeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PROJECT_LEAVE_REQUEST_NO")
    private Long projectLeaveRequestNo;

    // 근태 신청 (ATTENDANCE_REQUEST) - 1:0..1 관계
    // 하나의 근태 신청은 최대 하나의 프로젝트 휴재 신청만 가질 수 있음
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ATTENDANCE_REQUEST_NO", nullable = false, unique = true)
    private AttendanceRequest attendanceRequest;

    // 프로젝트 (PROJECT) - 1:N 관계
    // 하나의 프로젝트는 여러 휴재 신청과 연결될 수 있음
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROJECT_NO", nullable = false)
    private Project project;
}
