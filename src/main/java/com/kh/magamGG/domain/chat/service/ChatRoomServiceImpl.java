package com.kh.magamGG.domain.chat.service;

import com.kh.magamGG.domain.chat.dto.response.ChatRoomResponseDto;
import com.kh.magamGG.domain.chat.entity.ChatRoom;
import com.kh.magamGG.domain.chat.entity.ChatRoomMember;
import com.kh.magamGG.domain.chat.repository.ChatRoomMemberRepository;
import com.kh.magamGG.domain.chat.repository.ChatRoomRepository;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.domain.project.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final MemberRepository memberRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Override
    @Transactional
    public void approveMemberJoin(Long memberNo, Long agencyNo) {
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        member.setMemberStatus("ACTIVE"); // Dirty Checking 적용

        // 에이전시 전체방 조회 또는 생성 (ALL 타입)
        ChatRoom agencyAllRoom = chatRoomRepository.findByAgencyNoAndChatRoomTypeAndChatRoomStatus(agencyNo, "ALL", "Y")
                .orElseGet(() -> createAgencyAllRoom(agencyNo));

        saveChatRoomMemberIfAbsent(agencyAllRoom, member);
    }

    /**
     * 내가 참여 중인 '채팅방' 목록 조회 (프로젝트 정보 포함)
     */
    @Override
    @Transactional
    public List<ChatRoomResponseDto> getMyChatRooms(Long memberNo) {
        log.info("📋 채팅방 목록 조회 시작 - 회원번호: {}", memberNo);
        
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        // 1. 에이전시 소속이면 전체 채팅방에 자동 참여 처리
        if (member.getAgency() != null) {
            Long agencyNo = member.getAgency().getAgencyNo();
            
            // 에이전시 전체 채팅방 조회 (ALL 타입)
            List<ChatRoom> agencyAllRooms = chatRoomRepository.findAllByAgencyNoAndChatRoomTypeAndChatRoomStatus(
                agencyNo, "ALL", "Y");
            
            // 전체 채팅방이 없으면 생성
            if (agencyAllRooms.isEmpty()) {
                ChatRoom newAllRoom = createAgencyAllRoom(agencyNo);
                agencyAllRooms = List.of(newAllRoom);
                log.info("🏢 에이전시 전체 채팅방 생성 완료 - 에이전시: {}", agencyNo);
            }
            
            // 전체 채팅방에 자동 참여
            for (ChatRoom allRoom : agencyAllRooms) {
                saveChatRoomMemberIfAbsent(allRoom, member);
            }
            
            log.info("🏢 에이전시 전체 채팅방 자동 참여 완료 - 에이전시: {}, 전체방 개수: {}", 
                agencyNo, agencyAllRooms.size());
        }

        // 2. ChatRoomMember를 통해 내가 참여한 모든 방을 한 번에 가져옴
        List<ChatRoomMember> myRoomMappings = chatRoomMemberRepository.findAllByMemberOrderByChatRoomMemberJoinedAtDesc(member);
        log.info("📋 참여 중인 채팅방 개수: {}", myRoomMappings.size());

        List<ChatRoomResponseDto> result = myRoomMappings.stream()
                .map(mapping -> {
                    ChatRoom room = mapping.getChatRoom();
                    log.info("🏠 채팅방 정보 - 번호: {}, 이름: '{}', 타입: {}", 
                        room.getChatRoomNo(), room.getChatRoomName(), room.getChatRoomType());
                    
                    // 여기서 ChatRoomResponseDto.from(room, lastMessage, unreadCount) 등으로 변환
                    ChatRoomResponseDto dto = convertToDto(room, mapping.getLastReadChatNo());
                    log.info("📤 DTO 변환 결과 - 번호: {}, 이름: '{}'", dto.getChatRoomNo(), dto.getChatRoomName());
                    return dto;
                })
                .collect(Collectors.toList());
                
        log.info("✅ 채팅방 목록 조회 완료 - 반환 개수: {}", result.size());
        return result;
    }

    @Override
    @Transactional
    public void joinProjectChatRoom(Long projectNo, String projectName, Long memberNo) {
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new RuntimeException("회원 정보를 찾을 수 없습니다."));

        Long agencyNo = member.getAgency().getAgencyNo();

        // 프로젝트 방 조회 또는 생성
        ChatRoom projectRoom = chatRoomRepository.findByAgencyNoAndProjectNoAndChatRoomTypeAndChatRoomStatus(
                        agencyNo, projectNo, "PROJECT", "Y")
                .orElseGet(() -> createNewProjectRoom(agencyNo, projectNo, projectName));

        saveChatRoomMemberIfAbsent(projectRoom, member);
    }

    // --- Private Helper Methods ---

    private ChatRoom createAgencyAllRoom(Long agencyNo) {
        return chatRoomRepository.save(ChatRoom.builder()
                .chatRoomName("에이전시 전체 채팅방")
                .chatRoomType("ALL")
                .agencyNo(agencyNo)
                .chatRoomStatus("Y")
                .chatRoomCreatedAt(LocalDateTime.now())
                .build());
    }

    private ChatRoom createNewProjectRoom(Long agencyNo, Long projectNo, String projectName) {
        return chatRoomRepository.save(ChatRoom.builder()
                .chatRoomName(projectName)
                .chatRoomType("PROJECT")
                .agencyNo(agencyNo)
                .projectNo(projectNo)
                .chatRoomStatus("Y")
                .chatRoomCreatedAt(LocalDateTime.now())
                .build());
    }

    private void saveChatRoomMemberIfAbsent(ChatRoom room, Member member) {
        if (!chatRoomMemberRepository.existsByChatRoomAndMember(room, member)) {
            ChatRoomMember roomMember = ChatRoomMember.builder()
                    .chatRoom(room)
                    .member(member)
                    .chatRoomMemberJoinedAt(LocalDateTime.now())
                    .build();
            chatRoomMemberRepository.save(roomMember);
        }
    }

    // DTO 변환 로직 (예시)
    private ChatRoomResponseDto convertToDto(ChatRoom room, Long lastReadNo) {
        log.debug("🔄 DTO 변환 - 채팅방 번호: {}, 이름: '{}'", room.getChatRoomNo(), room.getChatRoomName());
        
        ChatRoomResponseDto dto = ChatRoomResponseDto.builder()
                .chatRoomNo(room.getChatRoomNo())
                .chatRoomName(room.getChatRoomName())
                .chatRoomType(room.getChatRoomType())
                .projectNo(room.getProjectNo())
                .lastMessage("") // TODO: 실제 마지막 메시지 조회
                .lastMessageTime("") // TODO: 실제 마지막 메시지 시간
                .unreadCount(0) // TODO: 실제 안 읽은 메시지 개수
                .build();
                
        log.debug("✅ DTO 변환 완료 - 결과 이름: '{}'", dto.getChatRoomName());
        return dto;
    }
}