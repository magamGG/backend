package com.kh.magamGG.domain.attendance.entity;

import com.kh.magamGG.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "leave_history")
@Getter
@Setter
@NoArgsConstructor
public class LeaveHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LEAVE_HISTORY_NO")
    private Long leaveHistoryNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_NO", nullable = false)
    private Member member;

    @Column(name = "LEAVE_HISTORY_DATE", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime leaveHistoryDate;

    @Column(name = "LEAVE_HISTORY_TYPE", nullable = false, length = 30)
    private String leaveHistoryType;

    @Column(name = "LEAVE_HISTORY_REASON", length = 1000)
    private String leaveHistoryReason;

    @Column(name = "LEAVE_HISTORY_AMOUNT")
    private Integer leaveHistoryAmount;

}
