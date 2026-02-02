package com.kh.magamGG.domain.health.entity;

import com.kh.magamGG.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "health_survey_response_item")
@Getter
@NoArgsConstructor
public class HealthSurveyResponseItem {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "HEALTH_SURVEY_RESPONSE_ITEM_NO")
	private Long healthSurveyResponseItemNo;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MEMBER_NO", nullable = false)
	private Member member;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "HEALTH_SURVEY_QUESTION_NO", nullable = false)
	private HealthSurveyQuestion healthSurveyQuestion;
	
	@Column(name = "HEALTH_SURVEY_QUESTION_ITEM_ANSWER_SCORE", nullable = false)
	private Integer healthSurveyQuestionItemAnswerScore;
	
	@Column(name = "HEALTH_SURVEY_QUESTION_ITEM_CREATED_AT", nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
	private LocalDateTime healthSurveyQuestionItemCreatedAt;

	// Setter methods
	public void setMember(Member member) {
		this.member = member;
	}

	public void setHealthSurveyQuestion(HealthSurveyQuestion healthSurveyQuestion) {
		this.healthSurveyQuestion = healthSurveyQuestion;
	}

	public void setHealthSurveyQuestionItemAnswerScore(Integer score) {
		this.healthSurveyQuestionItemAnswerScore = score;
	}

	public void setHealthSurveyQuestionItemCreatedAt(LocalDateTime createdAt) {
		this.healthSurveyQuestionItemCreatedAt = createdAt;
	}
}
