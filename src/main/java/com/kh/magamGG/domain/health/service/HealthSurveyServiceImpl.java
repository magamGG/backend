package com.kh.magamGG.domain.health.service;

import com.kh.magamGG.domain.health.dto.HealthSurveyQuestionResponseDto;
import com.kh.magamGG.domain.health.entity.HealthSurvey;
import com.kh.magamGG.domain.health.entity.HealthSurveyQuestion;
import com.kh.magamGG.domain.health.repository.HealthSurveyQuestionRepository;
import com.kh.magamGG.domain.health.repository.HealthSurveyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HealthSurveyServiceImpl implements HealthSurveyService {

    private final HealthSurveyQuestionRepository healthSurveyQuestionRepository;
    private final HealthSurveyRepository healthSurveyRepository;

    @Override
    public List<HealthSurveyQuestionResponseDto> getQuestionsBySurveyNo(Long healthSurveyNo) {
        List<HealthSurveyQuestion> questions =
            healthSurveyQuestionRepository.findByHealthSurvey_HealthSurveyNoOrderByHealthSurveyOrderAsc(healthSurveyNo);

        return questions.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<HealthSurveyQuestionResponseDto> getQuestionsBySurveyType(String healthSurveyType) {
        List<HealthSurveyQuestion> questions =
            healthSurveyQuestionRepository.findByHealthSurvey_HealthSurveyTypeOrderByHealthSurveyOrderAsc(healthSurveyType);

        return questions.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<HealthSurveyQuestionResponseDto> getQuestionsBySurveyName(String healthSurveyName) {
        HealthSurvey survey = healthSurveyRepository.findByHealthSurveyName(healthSurveyName)
            .orElseThrow(() -> new IllegalArgumentException("설문을 찾을 수 없습니다: " + healthSurveyName));

        return getQuestionsBySurveyNo(survey.getHealthSurveyNo());
    }

    private HealthSurveyQuestionResponseDto toDto(HealthSurveyQuestion question) {
        return HealthSurveyQuestionResponseDto.builder()
            .healthSurveyQuestionNo(question.getHealthSurveyQuestionNo())
            .healthSurveyNo(
                question.getHealthSurvey() != null
                    ? question.getHealthSurvey().getHealthSurveyNo()
                    : null
            )
            .healthSurveyOrder(question.getHealthSurveyOrder())
            .healthSurveyQuestionContent(question.getHealthSurveyQuestionContent())
            .healthSurveyQuestionMinScore(question.getHealthSurveyQuestionMinScore())
            .healthSurveyQuestionMaxScore(question.getHealthSurveyQuestionMaxScore())
            .build();
    }
}


