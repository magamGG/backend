package com.kh.magamGG.domain.health.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "HEALTH_SURVEY_RESPONSE_ITEM")
public class HealthSurveyResponseItem {
	
	@Id
	@Column(name = "HEALTH_SURVEY_RESPONSE_ITEM_NO")
	private Long healthSurveyResponseItemNo;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "HEALTH_SURVEY_RESPONSE_NO", nullable = false)
	private HealthSurveyResponse healthSurveyResponse;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "HEALTH_SURVEY_QUESTION_NO", nullable = false)
	private HealthSurveyQuestion healthSurveyQuestion;
	
	@Column(name = "HEALTH_SURVEY_QUESTION_ITEM_ANSWER_SCORE", nullable = false)
	private Long healthSurveyQuestionItemAnswerScore;
	
	@Column(name = "HEALTH_SURVEY_QUESTION_ITEM_CREATED_AT", nullable = false)
	private LocalDateTime healthSurveyQuestionItemCreatedAt;
}
