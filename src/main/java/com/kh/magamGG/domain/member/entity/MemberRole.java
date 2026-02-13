package com.kh.magamGG.domain.member.entity;

public enum MemberRole {
    웹툰작가("웹툰작가"),
    웹소설작가("웹소설작가"),
    어시스트_채색("어시스트 - 채색"),
    어시스트_조명("어시스트 - 조명"),
    어시스트_배경("어시스트 - 배경"),
    어시스트_선화("어시스트 - 선화"),
    어시스트_기타("어시스트 - 기타"),
    에이전시관리자("에이전시 관리자"),
    담당자("담당자"),
    USER("USER"); // 기본값

    private final String displayName;

    MemberRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // String 값으로부터 enum을 찾는 메서드
    public static MemberRole fromString(String role) {
        if (role == null || role.trim().isEmpty()) {
            return USER;
        }

        for (MemberRole memberRole : MemberRole.values()) {
            if (memberRole.displayName.equals(role) || memberRole.name().equals(role)) {
                return memberRole;
            }
        }
        return USER; // 매칭되지 않으면 기본값 반환
    }

    @Override
    public String toString() {
        return displayName;
    }
}