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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
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
        log.info("🔍 메시지 저장 서비스 시작");
        log.info("🔍 요청 DTO: chatRoomNo={}, memberNo={}, message='{}'", 
            chatMessageRequestDto.getChatRoomNo(), 
            chatMessageRequestDto.getMemberNo(), 
            chatMessageRequestDto.getChatMessage());
        
        try {
            log.info("🔍 채팅방 조회 시작 - ID: {}", chatMessageRequestDto.getChatRoomNo());
            ChatRoom room = chatRoomRepository.findById(chatMessageRequestDto.getChatRoomNo())
                    .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다. ID: " + chatMessageRequestDto.getChatRoomNo()));
            log.info("✅ 채팅방 조회 성공 - 이름: '{}'", room.getChatRoomName());

            log.info("🔍 회원 조회 시작 - ID: {}", chatMessageRequestDto.getMemberNo());
            Member member = memberRepository.findById(chatMessageRequestDto.getMemberNo())
                    .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다. ID: " + chatMessageRequestDto.getMemberNo()));
            log.info("✅ 회원 조회 성공 - 이름: '{}'", member.getMemberName());

            log.info("🔍 메시지 엔티티 생성 시작");
            ChatMessage message = ChatMessage.builder()
                    .chatRoom(room)
                    .member(member)
                    .chatMessage(chatMessageRequestDto.getChatMessage())
                    .chatMessageType(chatMessageRequestDto.getChatMessageType() != null ? chatMessageRequestDto.getChatMessageType() : "TEXT")
                    .chatStatus("Y")
                    .chatMessageCreatedAt(LocalDateTime.now())
                    .build();
            log.info("✅ 메시지 엔티티 생성 완료");

            log.info("🔍 메시지 저장 시작");
            ChatMessage saved = chatMessageRepository.save(message);
            log.info("✅ 메시지 저장 성공 - ID: {}", saved.getChatNo());

            log.info("🔍 응답 DTO 변환 시작");
            ChatMessageResponseDto response = ChatMessageResponseDto.from(saved);
            log.info("✅ 메시지 저장 서비스 완료 - 응답 ID: {}", response.getChatNo());
            
            return response;
        } catch (Exception e) {
            log.error("❌ 메시지 저장 실패 - 에러 타입: {}, 메시지: {}", e.getClass().getSimpleName(), e.getMessage());
            log.error("❌ 스택 트레이스:", e);
            throw e;
        }
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