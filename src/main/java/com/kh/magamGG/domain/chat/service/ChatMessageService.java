package com.kh.magamGG.domain.chat.service;

import com.kh.magamGG.domain.chat.dto.request.ChatMessageRequestDto;
import com.kh.magamGG.domain.chat.dto.response.ChatMessageResponseDto;
import com.kh.magamGG.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface ChatMessageService {


    ChatMessageResponseDto saveMessage(ChatMessageRequestDto chatMessageRequestDto);

    // 채팅 내역 조회 (Slice를 이용한 페이징) - 멤버 입장 시간 이후 메시지만
    Slice<ChatMessageResponseDto> getChatHistory(Long chatRoomNo, Long memberNo, Pageable pageable);

    // 안 읽은 메시지 수 확인
    long getUnreadCount(Long chatRoomNo, Long memberNo);
    
    // 파일 업로드
    String uploadFile(MultipartFile file, Long chatRoomNo, Long memberNo) throws Exception;
    
    // 파일 업로드 및 메시지 저장 (WebSocket 전송용)
    ChatMessageResponseDto uploadFileAndSaveMessage(MultipartFile file, Long chatRoomNo, Long memberNo) throws Exception;
    
    // 채팅 파일 다운로드
    org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> downloadChatFile(String fileName) throws Exception;
}