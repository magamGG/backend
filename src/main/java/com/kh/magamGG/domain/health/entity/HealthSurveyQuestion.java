package com.kh.magamGG.domain.health.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "HEALTH_SURVEY_QUESTION")
public class HealthSurveyQuestion {
	
	@Id
	@Column(name = "HEALTH_SURVEY_QUESTION_NO")
	private Long healthSurveyQuestionNo;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "HEALTH_SURVEY_NO", nullable = false)
	private HealthSurvey healthSurvey;
	
	@Column(name = "HEALTH_SURVEY_ORDER")
	private Long healthSurveyOrder;
	
	@Column(name = "HEALTH_SRUVEY_QUESTION_CONTENT", length = 1000)
	private String healthSruveyQuestionContent;
	
	@Column(name = "HEALTH_SURVEY_QUESTION_MIN_SCORE")
	private Long healthSurveyQuestionMinScore;
	
	@Column(name = "HEALTH_SURVEY_QUESTION_MAX_SCORE")
	private Long healthSurveyQuestionMaxScore;
	
	@Column(name = "HEALTH_SURVEY_QUESTION_CREATED_AT", nullable = false)
	private LocalDateTime healthSurveyQuestionCreatedAt;
	
	@Column(name = "HEALTH_SURVEY_QUESTION_UPDATED_AT")
	private LocalDateTime healthSurveyQuestionUpdatedAt;
	
	@OneToMany(mappedBy = "healthSurveyQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<HealthSurveyResponseItem> healthSurveyResponseItems = new ArrayList<>();
}
