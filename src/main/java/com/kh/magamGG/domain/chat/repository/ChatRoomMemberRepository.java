package com.kh.magamGG.domain.chat.repository;

import com.kh.magamGG.domain.chat.entity.ChatRoom;
import com.kh.magamGG.domain.chat.entity.ChatRoomMember;
import com.kh.magamGG.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    // 1. 특정 방에 특정 유저가 이미 존재하는지 확인 (Service에서 사용한 메서드)
    boolean existsByChatRoomAndMember(ChatRoom chatRoom, Member member);

    List<ChatRoomMember> findAllByMember_MemberNo(Long memberNo);

    // 2. 특정 유저가 속한 모든 채팅방 멤버 정보 조회
    // (보통 채팅방 목록을 불러올 때 사용하며, 최신순으로 정렬)
    List<ChatRoomMember> findAllByMemberOrderByChatRoomMemberJoinedAtDesc(Member member);

    // 3. 특정 채팅방에 참여 중인 유저 정보 조회
    Optional<ChatRoomMember> findByChatRoomAndMember(ChatRoom chatRoom, Member member);

    // 4. 특정 채팅방의 모든 참여자 목록 조회
    List<ChatRoomMember> findAllByChatRoom(ChatRoom chatRoom);

    // 5. 특정 채팅방에서 유저 탈퇴/강퇴 처리
    void deleteByChatRoomAndMember(ChatRoom chatRoom, Member member);
}