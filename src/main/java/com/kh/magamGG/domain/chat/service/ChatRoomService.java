package com.kh.magamGG.domain.chat.service;

import com.kh.magamGG.domain.chat.dto.response.ChatRoomResponseDto;

import java.util.List;
import java.util.Map;

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

    /**
     * 특정 채팅방의 참여자 목록 조회
     */
    List<Map<String, Object>> getChatRoomMembers(Long chatRoomNo);

    /**
     * 채팅 버튼 클릭 시 자동으로 채팅방 생성 및 참여자 초대
     */
    void ensureChatRoomsAndInviteMembers(Long memberNo);

    /**
     * 간단한 채팅방 멤버 로그 출력 (프로필 정보)
     */
    void logChatRoomMembers(Long chatRoomNo);

}