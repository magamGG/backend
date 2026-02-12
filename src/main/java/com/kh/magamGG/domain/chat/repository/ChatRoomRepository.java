package com.kh.magamGG.domain.chat.repository;

import com.kh.magamGG.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByAgencyNoAndChatRoomTypeAndChatRoomStatus(Long agencyNo, String type, String status);

    Optional<ChatRoom> findByAgencyNoAndProjectNoAndChatRoomTypeAndChatRoomStatus(Long agencyNo, Long projectNo, String type, String status);

}