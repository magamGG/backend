package com.kh.magamGG.domain.chat.repository;

import com.kh.magamGG.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByAgencyNoAndChatRoomTypeAndChatRoomStatus(Long agencyNo, String type, String status);

    Optional<ChatRoom> findByAgencyNoAndProjectNoAndChatRoomTypeAndChatRoomStatus(Long agencyNo, Long projectNo, String type, String status);

    List<ChatRoom> findByAgencyNoAndChatRoomStatusOrderByChatRoomCreatedAtDesc(Long agencyNo, String status);

    /**
     * 특정 멤버가 참여한 프로젝트의 채팅방들을 조회
     * 프로젝트 멤버 테이블과 조인해서 해당 멤버가 참여한 프로젝트의 채팅방만 반환
     */
    @Query("SELECT cr FROM ChatRoom cr " +
           "WHERE cr.agencyNo = :agencyNo " +
           "AND cr.chatRoomType = 'PROJECT' " +
           "AND cr.chatRoomStatus = 'Y' " +
           "AND cr.projectNo IN (" +
           "    SELECT pm.project.projectNo FROM ProjectMember pm " +
           "    WHERE pm.member.memberNo = :memberNo" +
           ") " +
           "ORDER BY cr.chatRoomCreatedAt DESC")
    List<ChatRoom> findProjectChatRoomsByMember(@Param("agencyNo") Long agencyNo, @Param("memberNo") Long memberNo);

}