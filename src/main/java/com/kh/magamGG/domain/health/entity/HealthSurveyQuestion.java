package com.kh.magamGG.domain.health.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "HEALTH_SURVEY_QUESTION")
@Getter
@NoArgsConstructor
public class HealthSurveyQuestion {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "HEALTH_SURVEY_QUESTION_NO")
	private Long healthSurveyQuestionNo;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "HEALTH_SURVEY_NO", nullable = false)
	private HealthSurvey healthSurvey;
	
	@Column(name = "HEALTH_SURVEY_ORDER")
	private Integer healthSurveyOrder;
	
	@Column(name = "HEALTH_SURVEY_QUESTION_CONTENT", length = 1000)
	private String healthSurveyQuestionContent;
	
	@Column(name = "HEALTH_SURVEY_QUESTION_MIN_SCORE")
	private Integer healthSurveyQuestionMinScore;
	
	@Column(name = "HEALTH_SURVEY_QUESTION_MAX_SCORE")
	private Integer healthSurveyQuestionMaxScore;
	
	@Column(name = "HEALTH_SURVEY_QUESTION_CREATED_AT", nullable = false)
	private LocalDateTime healthSurveyQuestionCreatedAt;
	
	@Column(name = "HEALTH_SURVEY_QUESTION_UPDATED_AT")
	private LocalDateTime healthSurveyQuestionUpdatedAt;
	
	@OneToMany(mappedBy = "healthSurveyQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<HealthSurveyResponseItem> healthSurveyResponseItems = new ArrayList<>();
}
