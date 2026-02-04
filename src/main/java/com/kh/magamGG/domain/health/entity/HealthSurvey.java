package com.kh.magamGG.domain.health.entity;

import com.kh.magamGG.domain.agency.entity.Agency;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "HEALTH_SURVEY")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthSurvey {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "HEALTH_SURVEY_NO")
	private Long healthSurveyNo;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "AGENCY_NO", nullable = false)
	private Agency agency;

	@Column(name = "HEALTH_SURVEY_PERIOD", columnDefinition = "INT DEFAULT 15")
	private Integer healthSurveyPeriod;

	@Column(name = "HEALTH_SURVEY_CYCLE", columnDefinition = "INT DEFAULT 30")
	private Integer healthSurveyCycle;

	@Column(name = "HEALTH_SURVEY_CREATED_AT", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
	private LocalDateTime healthSurveyCreatedAt;

	@Column(name = "HEALTH_SURVEY_UPDATED_AT", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
	private LocalDateTime healthSurveyUpdatedAt;
	
	// HealthSurveyQuestion 연관관계 제거 (소속 상관없이 모든 문항 공통 사용)
}

