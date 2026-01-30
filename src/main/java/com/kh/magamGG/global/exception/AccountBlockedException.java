package com.kh.magamGG.global.exception;

/**
 * 휴면/차단 계정인 경우
 */
public class AccountBlockedException extends RuntimeException {
    public AccountBlockedException(String message) {
        super(message);
    }
}
