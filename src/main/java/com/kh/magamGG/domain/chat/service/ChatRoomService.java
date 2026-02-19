package com.kh.magamGG.domain.chat.service;

import com.kh.magamGG.domain.chat.dto.response.ChatRoomResponseDto;

import java.util.List;

public interface ChatRoomService {

    void approveMemberJoin(Long memberNo, Long agencyNo);

    List<ChatRoomResponseDto> getMyChatRooms(Long memberNo);

    void joinProjectChatRoom(Long projectNo, String projectName, Long memberNo);

}
