package com.kh.magamGG.domain.chat.service;

import com.kh.magamGG.domain.chat.dto.request.ChatMessageRequestDto;
import com.kh.magamGG.domain.chat.dto.response.ChatMessageResponseDto;
import com.kh.magamGG.domain.chat.entity.ChatMessage;
import com.kh.magamGG.domain.chat.entity.ChatRoom;
import com.kh.magamGG.domain.chat.entity.ChatRoomMember;
import com.kh.magamGG.domain.chat.repository.ChatMessageRepository;
import com.kh.magamGG.domain.chat.repository.ChatRoomMemberRepository;
import com.kh.magamGG.domain.chat.repository.ChatRoomRepository;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본 조회용, 저장 메서드만 @Transactional 따로 부여
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final MemberRepository memberRepository;

    /**
     * 실시간 채팅 메시지 저장
     */
    @Override
    @Transactional
    public ChatMessageResponseDto saveMessage(ChatMessageRequestDto chatMessageRequestDto) {
        ChatRoom room = chatRoomRepository.findById(chatMessageRequestDto.getChatRoomNo())
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다. ID: " + chatMessageRequestDto.getChatRoomNo()));

        Member member = memberRepository.findById(chatMessageRequestDto.getMemberNo())
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다. ID: " + chatMessageRequestDto.getMemberNo()));

        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .member(member)
                .chatMessage(chatMessageRequestDto.getChatMessage())
                .chatMessageType(chatMessageRequestDto.getChatMessageType() != null ? chatMessageRequestDto.getChatMessageType() : "TEXT")
                .chatStatus("Y")
                .chatMessageCreatedAt(LocalDateTime.now())
                .build();

        ChatMessage saved = chatMessageRepository.save(message);

        return ChatMessageResponseDto.from(saved);
    }

    /**
     * 채팅 내역 조회 (무한 스크롤 최적화)
     */
    @Override
    public Slice<ChatMessageResponseDto> getChatHistory(Long chatRoomNo, Pageable pageable) {
        ChatRoom room = chatRoomRepository.findById(chatRoomNo)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));

        // 1. DB 조회 (결과는 Slice<ChatMessage> 엔티티 형태)
        Slice<ChatMessage> messages = chatMessageRepository.findAllByChatRoomAndChatStatusOrderByChatMessageCreatedAtDesc(
                room, "Y", pageable);

        // 2. 엔티티 Slice를 DTO Slice로 변환하여 반환
        return messages.map(ChatMessageResponseDto::from);
    }

    /**
     * 안 읽은 메시지 수 카운트
     */
    @Override
    public long getUnreadCount(Long chatRoomNo, Long memberNo) {
        ChatRoom room = chatRoomRepository.findById(chatRoomNo).orElseThrow();
        Member member = memberRepository.findById(memberNo).orElseThrow();

        // 참여 정보 조회
        ChatRoomMember roomMember = chatRoomMemberRepository.findByChatRoomAndMember(room, member)
                .orElseThrow(() -> new RuntimeException("해당 방의 참여 멤버가 아닙니다."));

        Long lastReadNo = roomMember.getLastReadChatNo();

        // 마지막으로 읽은 메시지 ID 이후의 메시지 개수를 리턴
        return chatMessageRepository.countByChatRoomAndChatNoGreaterThan(room,
                lastReadNo != null ? lastReadNo : 0L);
    }

}