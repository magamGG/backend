package com.kh.magamGG.domain.chat.repository;

import com.kh.magamGG.domain.chat.entity.ChatMessage;
import com.kh.magamGG.domain.chat.entity.ChatRoom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 1. 특정 채팅방의 메시지 내역 조회 (최신순)
    // Slice를 사용하면 '다음 페이지가 있는지' 여부만 확인하여 카운트 쿼리 부하를 줄일 수 있습니다 (무한 스크롤에 최적화).
    Slice<ChatMessage> findAllByChatRoomAndChatStatusOrderByChatMessageCreatedAtDesc(
            ChatRoom chatRoom, String status, Pageable pageable);

    // 2. 안 읽은 메시지 개수 카운트
    // 유저가 마지막으로 읽은 메시지 ID(lastReadChatNo)보다 큰 ID를 가진 메시지 개수를 셉니다.
    long countByChatRoomAndChatNoGreaterThan(ChatRoom chatRoom, Long lastReadChatNo);

    // 3. 채팅방 목록 미리보기용 최신 메시지 조회
    // 각 방의 마지막 대화 내용 하나만 가져올 때 사용합니다.
    Optional<ChatMessage> findFirstByChatRoomOrderByChatMessageCreatedAtDesc(ChatRoom chatRoom);
}