package com.kh.magamGG.domain.chat.repository;

import com.kh.magamGG.domain.chat.entity.ChatMessage;
import com.kh.magamGG.domain.chat.entity.ChatRoom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 1. 특정 채팅방의 메시지 내역 조회 (최신순)
    // Slice를 사용하면 '다음 페이지가 있는지' 여부만 확인하여 카운트 쿼리 부하를 줄일 수 있습니다 (무한 스크롤에 최적화).
    Slice<ChatMessage> findAllByChatRoomAndChatStatusOrderByChatMessageCreatedAtDesc(
            ChatRoom chatRoom, String status, Pageable pageable);

    // 1-1. 특정 채팅방의 메시지 내역 조회 (멤버 입장 시간 이후만)
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatRoom = :chatRoom AND cm.chatStatus = :status " +
           "AND cm.chatMessageCreatedAt >= :joinedAt ORDER BY cm.chatMessageCreatedAt DESC")
    Slice<ChatMessage> findAllByChatRoomAndChatStatusAndChatMessageCreatedAtGreaterThanEqualOrderByChatMessageCreatedAtDesc(
            @Param("chatRoom") ChatRoom chatRoom, 
            @Param("status") String status, 
            @Param("joinedAt") java.time.LocalDateTime joinedAt, 
            Pageable pageable);

    // 2-1. 안 읽은 메시지 개수 카운트 (멤버 입장 시간 이후만)
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.chatRoom = :chatRoom " +
           "AND cm.chatNo > :lastReadChatNo AND cm.chatMessageCreatedAt >= :joinedAt")
    long countByChatRoomAndChatNoGreaterThanAndChatMessageCreatedAtGreaterThanEqual(
            @Param("chatRoom") ChatRoom chatRoom, 
            @Param("lastReadChatNo") Long lastReadChatNo,
            @Param("joinedAt") java.time.LocalDateTime joinedAt);

    // 2. 안 읽은 메시지 개수 카운트
    // 유저가 마지막으로 읽은 메시지 ID(lastReadChatNo)보다 큰 ID를 가진 메시지 개수를 셉니다.
    long countByChatRoomAndChatNoGreaterThan(ChatRoom chatRoom, Long lastReadChatNo);

    // 3. 채팅방 목록 미리보기용 최신 메시지 조회
    // 각 방의 마지막 대화 내용 하나만 가져올 때 사용합니다.
    Optional<ChatMessage> findFirstByChatRoomOrderByChatMessageCreatedAtDesc(ChatRoom chatRoom);

    // 3-1. 멤버 입장 시간 이후의 최신 메시지 조회 (LIMIT 1로 고유성 보장)
    @Query(value = "SELECT cm.* FROM CHAT_MESSAGE cm WHERE cm.CHAT_ROOM_NO = :#{#chatRoom.chatRoomNo} " +
           "AND cm.CHAT_STATUS = 'Y' AND cm.CHAT_MESSAGE_CREATED_AT >= :joinedAt " +
           "ORDER BY cm.CHAT_MESSAGE_CREATED_AT DESC, cm.CHAT_NO DESC LIMIT 1", nativeQuery = true)
    Optional<ChatMessage> findFirstByChatRoomAndChatMessageCreatedAtGreaterThanEqualOrderByChatMessageCreatedAtDesc(
            @Param("chatRoom") ChatRoom chatRoom, 
            @Param("joinedAt") java.time.LocalDateTime joinedAt);
    // 4. 채팅방의 전체 메시지 개수 카운트
    long countByChatRoom(ChatRoom chatRoom);

    // 5. 최근 메시지 조회 (디버깅용) - 간단한 방식으로 변경
    List<ChatMessage> findTop5ByChatRoomOrderByChatMessageCreatedAtDesc(ChatRoom chatRoom);
}