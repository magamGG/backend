package com.kh.magamGG.domain.health.entity;

import com.kh.magamGG.domain.member.entity.Member;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "HEALTH_SURVEY_RESPONSE")
public class HealthSurveyResponse {
	
	@Id
	@Column(name = "HEALTH_SURVEY_RESPONSE_NO")
	private Long healthSurveyResponseNo;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "HEALTH_SURVEY_NO", nullable = false)
	private HealthSurvey healthSurvey;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MEMBER_NO", nullable = false)
	private Member member;
	
	@Column(name = "HEALTH_SURVEY_RESPONSE_TOTAL_SCORE")
	private Long healthSurveyResponseTotalScore;
	
	@Column(name = "HEALTH_SURVEY_RESPONSE_STATUS", nullable = false, length = 1)
	private String healthSurveyResponseStatus;
	
	@Column(name = "HEALTH_SURVEY_RESPONSE_CREATED_AT")
	private LocalDateTime healthSurveyResponseCreatedAt;
	
	@OneToMany(mappedBy = "healthSurveyResponse", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<HealthSurveyResponseItem> healthSurveyResponseItems = new ArrayList<>();
}
