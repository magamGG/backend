package com.kh.magamGG.domain.health.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "HEALTH_SURVEY_RESPONSE_ITEM")
@Getter
@NoArgsConstructor
public class HealthSurveyResponseItem {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "HEALTH_SURVEY_RESPONSE_ITEM_NO")
	private Long healthSurveyResponseItemNo;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "HEALTH_SURVEY_RESPONSE_NO", nullable = false)
	private HealthSurveyResponse healthSurveyResponse;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "HEALTH_SURVEY_QUESTION_NO", nullable = false)
	private HealthSurveyQuestion healthSurveyQuestion;
	
	@Column(name = "HEALTH_SURVEY_QUESTION_ITEM_ANSWER_SCORE", nullable = false)
	private Integer healthSurveyQuestionItemAnswerScore;
	
	@Column(name = "HEALTH_SURVEY_QUESTION_ITEM_CREATED_AT", nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
	private LocalDateTime healthSurveyQuestionItemCreatedAt;
}
