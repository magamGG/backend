package com.kh.magamGG.global.exception;

/**
 * 프로젝트 접근 권한이 없을 때 발생
 * (PROJECT_MEMBER에 포함된 회원만 프로젝트 조회 가능)
 */
public class ProjectAccessDeniedException extends RuntimeException {

    public ProjectAccessDeniedException(String message) {
        super(message);
    }
}
