package com.kh.magamGG.domain.health.entity;

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
	
	@Column(name = "HEALTH_SURVEY_STATUS", nullable = false, length = 1)
	private String healthSurveyStatus;
	
	@Column(name = "HEALTH_SURVEY_CREATED_AT")
	private LocalDateTime healthSurveyCreatedAt;
	
	@Column(name = "HEALTH_SURVEY_UPDATED_AT")
	private LocalDateTime healthSurveyUpdatedAt;
	
	@OneToMany(mappedBy = "healthSurvey", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<HealthSurveyQuestion> healthSurveyQuestions = new ArrayList<>();
	
	@OneToMany(mappedBy = "healthSurvey", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<HealthSurveyResponse> healthSurveyResponses = new ArrayList<>();
}
