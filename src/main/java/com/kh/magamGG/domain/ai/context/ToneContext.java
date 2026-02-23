package com.kh.magamGG.domain.ai.context;

/**
 * 요청 단위로 챗봇 말투(tone)를 전달하기 위한 컨텍스트.
 * Controller에서 쿼리 파라미터 tone을 설정하고, Service의 callAi에서 읽어 사용.
 */
public final class ToneContext {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    public static void set(String tone) {
        HOLDER.set(tone);
    }

    public static String get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    private ToneContext() {}
}
