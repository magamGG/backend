package com.kh.magamGG.domain.health.entity;

import com.kh.magamGG.domain.agency.entity.Agency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "health_survey")
@Getter
@NoArgsConstructor
public class HealthSurvey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HEALTH_SURVEY_NO")
    private Long healthSurveyNo;

    // 건강 설문 주기 관련 컬럼
    @Column(name = "HEALTH_SURVEY_PERIOD", columnDefinition = "INT DEFAULT 15")
    private Integer healthSurveyPeriod;

    @Column(name = "HEALTH_SURVEY_CYCLE", columnDefinition = "INT DEFAULT 30")
    private Integer healthSurveyCycle;

    @Column(name = "HEALTH_SURVEY_CREATED_AT", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime healthSurveyCreatedAt;

    @Column(name = "HEALTH_SURVEY_UPDATED_AT", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime healthSurveyUpdatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AGENCY_NO", nullable = false)
    private Agency agency;

    @OneToMany(mappedBy = "healthSurvey", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HealthSurveyQuestion> healthSurveyQuestions = new ArrayList<>();
}



