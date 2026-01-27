package com.kh.magamGG.domain.health.entity;

import com.kh.magamGG.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Daily_Health_Check")
@Getter
@NoArgsConstructor
public class DailyHealthCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DAILY_HEALTH_NO")
    private Long dailyHealthNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_NO", nullable = false)
    private Member member;

    @Column(name = "HEALTH_CONDITION", length = 20)
    private String healthCondition;

    @Column(name = "SLEEP_HOURS")
    private Integer sleepHours;

    @Column(name = "DISCOMFORT_LEVEL")
    private Integer discomfortLevel;

    @Column(name = "HEALTH_CHECK_NOTES", length = 1000)
    private Long healthCheckNotes;

    @Column(name = "HEALTH_CHECK_CREATED_AT", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime healthCheckCreatedAt;
}
