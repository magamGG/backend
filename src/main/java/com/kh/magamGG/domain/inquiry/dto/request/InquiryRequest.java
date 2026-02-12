package com.kh.magamGG.domain.inquiry.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InquiryRequest {
    
    @NotBlank(message = "문의 유형을 선택해주세요.")
    private String inquiryType; // bug, feature, system, other
    
    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
    private String title;
    
    @NotBlank(message = "내용을 입력해주세요.")
    @Size(max = 500, message = "내용은 500자 이하여야 합니다.")
    private String content;
    
    // developerEmail은 백엔드에서 고정값으로 설정하거나, 프론트에서 전달
    private String developerEmail; // 선택적 (없으면 백엔드에서 기본값 사용)
}

