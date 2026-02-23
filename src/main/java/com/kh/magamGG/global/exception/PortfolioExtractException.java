package com.kh.magamGG.global.exception;

/**
 * 포트폴리오 추출 실패 (이미지/URL에서 정보를 추출하지 못한 경우)
 * 500 대신 200 + success: false 로 응답해 프론트에서 안내 메시지 표시용
 */
public class PortfolioExtractException extends RuntimeException {

    public PortfolioExtractException(String message) {
        super(message);
    }
}
