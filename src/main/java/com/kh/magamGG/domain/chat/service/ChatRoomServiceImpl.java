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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

        // 에이전시 전체방 조회 또는 생성
        ChatRoom agencyTotalRoom = chatRoomRepository.findByAgencyNoAndChatRoomTypeAndChatRoomStatus(agencyNo, "TOTAL", "Y")
                .orElseGet(() -> createAgencyTotalRoom(agencyNo));

        saveChatRoomMemberIfAbsent(agencyTotalRoom, member);
    }

    /**
     * 내가 참여 중인 '채팅방' 목록 조회 (프로젝트 정보 포함)
     */
    @Override
    public List<ChatRoomResponseDto> getMyChatRooms(Long memberNo) {
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        // 2. ChatRoomMember를 통해 내가 참여한 모든 방을 한 번에 가져옴
        List<ChatRoomMember> myRoomMappings = chatRoomMemberRepository.findAllByMemberOrderByChatRoomMemberJoinedAtDesc(member);

        return myRoomMappings.stream()
                .map(mapping -> {
                    ChatRoom room = mapping.getChatRoom();
                    // 여기서 ChatRoomResponseDto.from(room, lastMessage, unreadCount) 등으로 변환
                    return convertToDto(room, mapping.getLastReadChatNo());
                })
                .collect(Collectors.toList());
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

    private ChatRoom createAgencyTotalRoom(Long agencyNo) {
        return chatRoomRepository.save(ChatRoom.builder()
                .chatRoomName("에이전시 전체 채팅방")
                .chatRoomType("TOTAL")
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
        return ChatRoomResponseDto.builder()
                .chatRoomNo(room.getChatRoomNo())
                .chatRoomName(room.getChatRoomName())
                .chatRoomType(room.getChatRoomType())
                .projectNo(room.getProjectNo())
                .build();
    }
}