package com.kh.magamGG.domain.notification.repository;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE Emitter 메모리 저장소.
 * memberNo_timestamp 형태로 다중 기기 접속 지원.
 */
@Repository
public class EmitterRepository {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter save(String emitterId, SseEmitter sseEmitter) {
        emitters.put(emitterId, sseEmitter);
        return sseEmitter;
    }

    public void deleteById(String emitterId) {
        emitters.remove(emitterId);
    }

    /**
     * memberNo에 해당하는 모든 Emitter 조회 (다중 기기 접속 시)
     */
    public Map<String, SseEmitter> findAllEmitterStartWithByMemberNo(String memberNo) {
        Map<String, SseEmitter> result = new ConcurrentHashMap<>();
        emitters.forEach((key, emitter) -> {
            if (key.startsWith(memberNo + "_")) {
                result.put(key, emitter);
            }
        });
        return result;
    }
}
