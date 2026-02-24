package com.kh.magamGG.domain.portfolio.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 포트폴리오 이미지/URL에서 AI가 추출한 구조화 데이터
 */
public record PortfolioExtractDto(
        @JsonProperty("name") String name,
        @JsonProperty("role") String role,
        @JsonProperty("email") String email,
        @JsonProperty("phone") String phone,
        @JsonProperty("projects") @JsonAlias("project") List<String> projects,
        @JsonProperty("careerItems") @JsonAlias({"career_items", "careerItems"}) List<String> careerItems,
        @JsonProperty("career") String career,
        @JsonProperty("workStyle") @JsonAlias({"work_style", "workStyle"}) List<String> workStyle,
        @JsonProperty("skills") List<String> skills
) {}
