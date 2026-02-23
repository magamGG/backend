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
     * @return 실제로 DB가 갱신되면 true, 같은 값으로 건너뛴 경우 false
     */
    boolean updateLastReadMessage(Long chatRoomNo, Long memberNo, Long lastChatNo);

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

    /**
     * 사용자의 마지막 읽은 메시지 번호 조회
     */
    Long getLastReadChatNo(Long chatRoomNo, Long memberNo);

    /**
     * 특정 메시지를 아직 읽지 않은 참여 중인 멤버 수 (카카오톡 스타일)
     * @param senderMemberNo 발신자 제외 시 해당 멤버 번호, 제외하지 않으면 null
     */
    long getUnreadMemberCount(Long chatRoomNo, Long chatNo, Long senderMemberNo);
}