package com.kh.magamGG.domain.portfolio.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 포트폴리오 이미지/URL에서 AI가 추출한 구조화 데이터 (마감지기용)
 * - 이름, 직무, 메일, 전화번호, 참여 프로젝트, 경력(항목별), 작업 스타일, 사용 기술
 * - Vision이 한글 키로 응답해도 파싱되도록 @JsonAlias 사용
 */
public record PortfolioExtractDto(
        @JsonProperty("name") @JsonAlias({"이름", "이름 (name)"}) String name,
        @JsonProperty("role") @JsonAlias({"직무", "직위", "역할"}) String role,
        @JsonProperty("email") @JsonAlias({"이메일", "이메일 주소", "메일"}) String email,
        @JsonProperty("phone") @JsonAlias({"전화", "전화번호", "연락처"}) String phone,
        @JsonProperty("projects") @JsonAlias({"project", "프로젝트", "참여 프로젝트"}) List<String> projects,
        @JsonProperty("careerItems") @JsonAlias({"career_items", "careerItems", "경력항목", "경력 항목", "경력"}) List<String> careerItems,
        @JsonProperty("career") @JsonAlias({"경력 전체", "경력 요약"}) String career,
        @JsonProperty("workStyle") @JsonAlias({"work_style", "workStyle", "작업스타일", "작업 스타일"}) List<String> workStyle,
        @JsonProperty("skills") @JsonAlias({"스킬", "기술", "사용 기술", "기술 스택"}) List<String> skills
) {}
