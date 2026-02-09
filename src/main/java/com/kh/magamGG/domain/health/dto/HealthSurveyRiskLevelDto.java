package com.kh.magamGG.domain.health.dto;

/**
 * 건강 설문 위험도 등급
 */
public class HealthSurveyRiskLevelDto {
    
    public static final String NORMAL = "정상";
    public static final String CAUTION = "주의";
    public static final String WARNING = "경고";
    public static final String DANGER = "위험";
    
    private HealthSurveyRiskLevelDto() {
        // 인스턴스 생성 방지
    }
}

