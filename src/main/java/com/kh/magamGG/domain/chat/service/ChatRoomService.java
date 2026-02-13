package com.kh.magamGG.domain.chat.service;

import com.kh.magamGG.domain.chat.dto.response.ChatRoomResponseDto;

import java.util.List;

public interface ChatRoomService {

    void approveMemberJoin(Long memberNo, Long agencyNo);

    List<ChatRoomResponseDto> getMyChatRooms(Long memberNo);

    List<ChatRoomResponseDto> getChatRoomsByAgency(Long agencyNo, String type, Long memberNo);

    void joinChatRoom(Long chatRoomNo, Long memberNo);

    /**
     * 마지막으로 읽은 메시지 업데이트
     */
    void updateLastReadMessage(Long chatRoomNo, Long memberNo, Long lastChatNo);

    /**
     * 특정 채팅방의 읽지 않은 메시지 개수 조회
     */
    long getUnreadCount(Long chatRoomNo, Long memberNo);

    void joinProjectChatRoom(Long projectNo, String projectName, Long memberNo);

    /**
     * 프로젝트 생성 시 채팅방 생성 및 멤버 추가
     */
    void createProjectChatRoom(Long projectNo, String projectName, Long agencyNo, List<Long> memberNos);

}