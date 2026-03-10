package com.kh.magamGG.global.exception;

import lombok.Getter;

@Getter
public class OAuthRegistrationRequiredException extends RuntimeException {
    private final String email;
    private final String name;
    private final String provider;
    
    public OAuthRegistrationRequiredException(String email, String name, String provider) {
        super("OAuth 회원가입이 필요합니다.");
        this.email = email;
        this.name = name;
        this.provider = provider;
    }
}

