package com.kh.magamGG.domain.health.entity;

import com.kh.magamGG.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "HEALTH_SURVEY_RESPONSE")
@Getter
@NoArgsConstructor
public class HealthSurveyResponse {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "HEALTH_SURVEY_RESPONSE_NO")
	private Long healthSurveyResponseNo;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MEMBER_NO", nullable = false)
	private Member member;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "HEALTH_SURVEY_NO", nullable = false)
	private HealthSurvey healthSurvey;
	
	@Column(name = "HEALTH_SURVEY_RESPONSE_TOTAL_SCORE", nullable = false)
	private Integer totalScore;
	
	@Column(name = "HEALTH_SURVEY_RESPONSE_CREATED_AT", nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
	private LocalDateTime healthSurveyResponseCreatedAt;

	// Setter methods
	public void setMember(Member member) {
		this.member = member;
	}

	public void setHealthSurvey(HealthSurvey healthSurvey) {
		this.healthSurvey = healthSurvey;
	}

	public void setTotalScore(Integer totalScore) {
		this.totalScore = totalScore;
	}

	public void setHealthSurveyResponseCreatedAt(LocalDateTime createdAt) {
		this.healthSurveyResponseCreatedAt = createdAt;
	}
}
