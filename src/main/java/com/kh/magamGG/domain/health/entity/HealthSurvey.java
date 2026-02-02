package com.kh.magamGG.domain.health.entity;

import com.kh.magamGG.domain.agency.entity.Agency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "HEALTH_SURVEY")
@Getter
@NoArgsConstructor
public class HealthSurvey {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "HEALTH_SURVEY_NO")
	private Long healthSurveyNo;
	
	@Column(name = "HEALTH_SURVEY_NAME", length = 100)
	private String healthSurveyName;
	
	@Column(name = "HEALTH_SURVEY_TYPE", length = 30)
	private String healthSurveyType;
	
	@Column(name = "HEALTH_SURVEY_CONTENT", length = 30)
	private String healthSurveyContent;
	
	@Column(name = "HEALTH_SURVEY_STATUS", nullable = false, columnDefinition = "VARCHAR(1) DEFAULT 'Y'")
	private String healthSurveyStatus;

    @Column(name = "HEALTH_SURVEY_PERIOD", columnDefinition = "INT DEFAULT 15")
    private Integer healthsSurveyPeriod;

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
