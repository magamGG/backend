package com.kh.magamGG.global.exception;

/**
 * 이미 오늘 출근 처리된 경우
 */
public class AlreadyCheckedInException extends RuntimeException {
    public AlreadyCheckedInException(String message) {
        super(message);
    }
}
