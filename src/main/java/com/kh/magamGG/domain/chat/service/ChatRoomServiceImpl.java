package com.kh.magamGG.domain.chat.service;

import com.kh.magamGG.domain.chat.dto.response.ChatRoomResponseDto;
import com.kh.magamGG.domain.chat.entity.ChatMessage;
import com.kh.magamGG.domain.chat.entity.ChatRoom;
import com.kh.magamGG.domain.chat.entity.ChatRoomMember;
import com.kh.magamGG.domain.chat.repository.ChatMessageRepository;
import com.kh.magamGG.domain.chat.repository.ChatRoomMemberRepository;
import com.kh.magamGG.domain.chat.repository.ChatRoomRepository;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.domain.project.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Override
    @Transactional
    public void approveMemberJoin(Long memberNo, Long agencyNo) {
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        member.setMemberStatus("ACTIVE"); // Dirty Checking 적용

        // 에이전시 전체방 조회 또는 생성
        ChatRoom agencyTotalRoom = chatRoomRepository.findByAgencyNoAndChatRoomTypeAndChatRoomStatus(agencyNo, "ALL", "Y")
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

    /**
     * 에이전시별 채팅방 목록 조회 (사용자별 필터링)
     * type이 "all"이면 해당 에이전시의 채팅방 중 사용자가 참여할 수 있는 채팅방들을 반환
     * - ALL 타입: 에이전시 전체 채팅방 (모든 에이전시 멤버 참여 가능)
     * - PROJECT 타입: 해당 프로젝트에 참여한 멤버만 참여 가능
     */
    @Override
    @Transactional
    public List<ChatRoomResponseDto> getChatRoomsByAgency(Long agencyNo, String type, Long memberNo) {
        if ("all".equals(type)) {
            Member member = memberRepository.findById(memberNo)
                    .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다: " + memberNo));
            
            // 1. 에이전시 전체 채팅방 조회 (ALL 타입)
            List<ChatRoom> allRooms = new ArrayList<>();
            chatRoomRepository.findByAgencyNoAndChatRoomTypeAndChatRoomStatus(agencyNo, "ALL", "Y")
                    .ifPresent(allRooms::add);
            
            // 2. 내가 참여한 프로젝트의 채팅방들 조회 (PROJECT 타입)
            List<ChatRoom> projectRooms = chatRoomRepository.findProjectChatRoomsByMember(agencyNo, memberNo);
            
            // 3. 두 리스트를 합치고 생성일 역순으로 정렬
            List<ChatRoom> combinedRooms = new ArrayList<>();
            combinedRooms.addAll(allRooms);
            combinedRooms.addAll(projectRooms);
            
            combinedRooms.sort((a, b) -> b.getChatRoomCreatedAt().compareTo(a.getChatRoomCreatedAt()));
            
            return combinedRooms.stream()
                    .map(room -> convertToDtoWithUnreadCount(room, memberNo))
                    .collect(Collectors.toList());
        } else {
            // 기본적으로는 빈 리스트 반환 (추후 다른 타입 추가 가능)
            return List.of();
        }
    }

    /**
     * 채팅방 입장 시 멤버 자동 등록
     */
    @Override
    @Transactional
    public void joinChatRoom(Long chatRoomNo, Long memberNo) {
        System.out.println("🔵 [DEBUG] joinChatRoom 시작: chatRoomNo=" + chatRoomNo + ", memberNo=" + memberNo);
        
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomNo)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
        
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));
        
        // 채팅방 멤버로 등록 (중복 체크 포함)
        saveChatRoomMemberIfAbsent(chatRoom, member);
        
        // 현재 lastReadChatNo 확인
        chatRoomMemberRepository.findByChatRoomAndMember(chatRoom, member)
                .ifPresent(roomMember -> {
                    System.out.println("🔍 [DEBUG] 현재 lastReadChatNo: " + roomMember.getLastReadChatNo());
                });
    }

    /**
     * 마지막으로 읽은 메시지 업데이트
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLastReadMessage(Long chatRoomNo, Long memberNo, Long lastChatNo) {
        System.out.println("🔵 [DEBUG] updateLastReadMessage 시작: chatRoomNo=" + chatRoomNo + 
                          ", memberNo=" + memberNo + ", lastChatNo=" + lastChatNo);
        
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomNo)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
        
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));
        
        // 채팅방 멤버 정보 조회
        ChatRoomMember roomMember = chatRoomMemberRepository.findByChatRoomAndMember(chatRoom, member)
                .orElseThrow(() -> new RuntimeException("채팅방 멤버가 아닙니다."));
        
        System.out.println("🔍 [DEBUG] 기존 lastReadChatNo: " + roomMember.getLastReadChatNo());
        System.out.println("🔍 [DEBUG] 업데이트할 lastChatNo: " + lastChatNo);
        
        // 같은 값으로 업데이트하려고 하는지 확인
        Long currentLastReadChatNo = roomMember.getLastReadChatNo();
        if (currentLastReadChatNo != null && currentLastReadChatNo.equals(lastChatNo)) {
            System.out.println("⚠️ [DEBUG] 같은 값으로 업데이트 시도! 기존: " + currentLastReadChatNo + ", 새로운: " + lastChatNo);
            System.out.println("⚠️ [DEBUG] 업데이트를 건너뛰고 현재 unread count 확인");
            
            // 현재 unread count 확인
            long currentUnreadCount = chatMessageRepository.countByChatRoomAndChatNoGreaterThan(chatRoom, currentLastReadChatNo);
            System.out.println("🔍 [DEBUG] 현재 unread count: " + currentUnreadCount);
            return;
        }
        
        // 마지막 읽은 메시지 번호 업데이트
        Long oldLastReadChatNo = roomMember.getLastReadChatNo();
        roomMember.setLastReadChatNo(lastChatNo);
        
        // 명시적으로 save 호출 및 flush로 즉시 DB 반영
        ChatRoomMember savedRoomMember = chatRoomMemberRepository.save(roomMember);
        chatRoomMemberRepository.flush(); // 즉시 DB에 반영
        
        System.out.println("🔍 [DEBUG] 저장 후 확인 - 기존: " + oldLastReadChatNo + " → 새로운: " + savedRoomMember.getLastReadChatNo());
        
        // DB에서 다시 조회해서 실제로 업데이트되었는지 확인
        ChatRoomMember verifyRoomMember = chatRoomMemberRepository.findByChatRoomAndMember(chatRoom, member)
                .orElseThrow(() -> new RuntimeException("검증용 조회 실패"));
        
        System.out.println("🔍 [DEBUG] DB 재조회 결과 lastReadChatNo: " + verifyRoomMember.getLastReadChatNo());
        
        if (!lastChatNo.equals(verifyRoomMember.getLastReadChatNo())) {
            System.out.println("❌ [ERROR] DB 업데이트 실패! 예상: " + lastChatNo + ", 실제: " + verifyRoomMember.getLastReadChatNo());
            throw new RuntimeException("DB 업데이트 실패");
        } else {
            System.out.println("✅ [DEBUG] DB 업데이트 성공 확인");
        }
        
        // 업데이트 후 unread count 재계산
        long newUnreadCount = chatMessageRepository.countByChatRoomAndChatNoGreaterThan(chatRoom, lastChatNo);
        System.out.println("🔍 [DEBUG] 업데이트 후 새로운 unread count: " + newUnreadCount);
        
        System.out.println("✅ [DEBUG] 마지막 읽은 메시지 업데이트 완료: chatRoomNo=" + chatRoomNo + 
                          ", memberNo=" + memberNo + ", 새로운 lastChatNo=" + lastChatNo);
    }

    /**
     * 특정 채팅방의 읽지 않은 메시지 개수 조회
     */
    @Override
    public long getUnreadCount(Long chatRoomNo, Long memberNo) {
        System.out.println("🔵 [DEBUG] getUnreadCount 시작: chatRoomNo=" + chatRoomNo + ", memberNo=" + memberNo);
        
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomNo)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
        
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));
        
        // 채팅방 멤버 정보 조회
        Optional<ChatRoomMember> roomMemberOpt = chatRoomMemberRepository.findByChatRoomAndMember(chatRoom, member);
        
        if (roomMemberOpt.isPresent()) {
            Long lastReadChatNo = roomMemberOpt.get().getLastReadChatNo();
            long unreadCount;
            
            System.out.println("🔍 [DEBUG] 현재 lastReadChatNo: " + lastReadChatNo);
            
            if (lastReadChatNo != null) {
                unreadCount = chatMessageRepository.countByChatRoomAndChatNoGreaterThan(chatRoom, lastReadChatNo);
                System.out.println("🔍 [DEBUG] countByChatRoomAndChatNoGreaterThan 쿼리 결과: " + unreadCount);
                System.out.println("🔍 [DEBUG] 쿼리 조건: chatRoom=" + chatRoom.getChatRoomNo() + ", lastReadChatNo > " + lastReadChatNo);
                
                // 실제 메시지들 확인
                System.out.println("🔍 [DEBUG] 채팅방 " + chatRoomNo + "의 최근 메시지들 확인 중...");
                // 최근 5개 메시지 조회해서 chat_no 확인
                try {
                    var recentMessages = chatMessageRepository.findTop5ByChatRoomOrderByChatMessageCreatedAtDesc(chatRoom);
                    System.out.println("🔍 [DEBUG] 최근 " + recentMessages.size() + "개 메시지의 chat_no:");
                    for (var msg : recentMessages) {
                        System.out.println("  - chat_no: " + msg.getChatNo() + ", 내용: " + msg.getChatMessage() + 
                                         ", 작성자: " + msg.getMember().getMemberName() + 
                                         ", 시간: " + msg.getChatMessageCreatedAt());
                    }
                } catch (Exception e) {
                    System.out.println("🔍 [DEBUG] 최근 메시지 조회 실패: " + e.getMessage());
                }
            } else {
                // 한 번도 읽지 않았다면 모든 메시지가 읽지 않은 메시지
                unreadCount = chatMessageRepository.countByChatRoom(chatRoom);
                System.out.println("🔍 [DEBUG] countByChatRoom 쿼리 결과 (처음 입장): " + unreadCount);
            }
            
            System.out.println("🔍 [DEBUG] getUnreadCount 결과: chatRoomNo=" + chatRoomNo + 
                             ", memberNo=" + memberNo + ", lastReadChatNo=" + lastReadChatNo + 
                             ", unreadCount=" + unreadCount);
            return unreadCount;
        } else {
            // 채팅방 멤버가 아니면 읽지 않은 메시지 개수는 0
            System.out.println("🔍 [DEBUG] 채팅방 멤버가 아님: chatRoomNo=" + chatRoomNo + ", memberNo=" + memberNo);
            return 0;
        }
    }

    /**
     * 프로젝트 생성 시 채팅방 생성 및 멤버 추가
     */
    @Override
    @Transactional
    public void createProjectChatRoom(Long projectNo, String projectName, Long agencyNo, List<Long> memberNos) {
        // 1. 프로젝트 채팅방 생성
        ChatRoom projectRoom = createNewProjectRoom(agencyNo, projectNo, projectName);
        
        // 2. 프로젝트 참여자들을 채팅방에 추가
        for (Long memberNo : memberNos) {
            Member member = memberRepository.findById(memberNo)
                    .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다: " + memberNo));
            saveChatRoomMemberIfAbsent(projectRoom, member);
        }
    }

    // --- Private Helper Methods ---

    private ChatRoom createAgencyTotalRoom(Long agencyNo) {
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

    // DTO 변환 로직 개선 - 실제 마지막 메시지와 시간 포함
    private ChatRoomResponseDto convertToDto(ChatRoom room, Long lastReadNo) {
        // 마지막 메시지 조회
        Optional<ChatMessage> lastMessageOpt = chatMessageRepository.findFirstByChatRoomOrderByChatMessageCreatedAtDesc(room);
        
        String lastMessage = "";
        String lastMessageTime = "";
        Long lastMessageSenderNo = null;
        String lastMessageSenderName = "";
        long unreadCount = 0;
        
        if (lastMessageOpt.isPresent()) {
            ChatMessage lastMsg = lastMessageOpt.get();
            lastMessage = lastMsg.getChatMessage();
            lastMessageSenderNo = lastMsg.getMember().getMemberNo();
            lastMessageSenderName = lastMsg.getMember().getMemberName();
            
            // 시간 포맷팅 (예: "오후 2:30" 또는 "2월 12일")
            LocalDateTime msgTime = lastMsg.getChatMessageCreatedAt();
            LocalDateTime now = LocalDateTime.now();
            
            if (msgTime.toLocalDate().equals(now.toLocalDate())) {
                // 오늘 메시지면 시간만 표시
                lastMessageTime = msgTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            } else {
                // 다른 날이면 날짜 표시
                lastMessageTime = msgTime.format(DateTimeFormatter.ofPattern("M월 d일"));
            }
            
            // 읽지 않은 메시지 개수 계산 (lastReadNo가 있을 때만)
            if (lastReadNo != null) {
                unreadCount = chatMessageRepository.countByChatRoomAndChatNoGreaterThan(room, lastReadNo);
                System.out.println("🔍 [DEBUG] 읽지 않은 메시지 계산: chatRoomNo=" + room.getChatRoomNo() + 
                                 ", lastReadNo=" + lastReadNo + ", unreadCount=" + unreadCount);
            } else {
                System.out.println("🔍 [DEBUG] lastReadNo가 null이므로 unreadCount 계산 안 함: chatRoomNo=" + room.getChatRoomNo());
            }
        }
        
        return ChatRoomResponseDto.builder()
                .chatRoomNo(room.getChatRoomNo())
                .chatRoomName(room.getChatRoomName())
                .chatRoomType(room.getChatRoomType())
                .projectNo(room.getProjectNo())
                .lastMessage(lastMessage)
                .lastMessageTime(lastMessageTime)
                .lastMessageSenderNo(lastMessageSenderNo)
                .lastMessageSenderName(lastMessageSenderName)
                .unreadCount(unreadCount)
                .build();
    }

    // 사용자별 읽지 않은 메시지 개수를 포함한 DTO 변환
    private ChatRoomResponseDto convertToDtoWithUnreadCount(ChatRoom room, Long memberNo) {
        // 마지막 메시지 조회
        Optional<ChatMessage> lastMessageOpt = chatMessageRepository.findFirstByChatRoomOrderByChatMessageCreatedAtDesc(room);
        
        String lastMessage = "";
        String lastMessageTime = "";
        Long lastMessageSenderNo = null;
        String lastMessageSenderName = "";
        long unreadCount = 0;
        
        if (lastMessageOpt.isPresent()) {
            ChatMessage lastMsg = lastMessageOpt.get();
            lastMessage = lastMsg.getChatMessage();
            lastMessageSenderNo = lastMsg.getMember().getMemberNo();
            lastMessageSenderName = lastMsg.getMember().getMemberName();
            
            // 시간 포맷팅 (예: "오후 2:30" 또는 "2월 12일")
            LocalDateTime msgTime = lastMsg.getChatMessageCreatedAt();
            LocalDateTime now = LocalDateTime.now();
            
            if (msgTime.toLocalDate().equals(now.toLocalDate())) {
                // 오늘 메시지면 시간만 표시
                lastMessageTime = msgTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            } else {
                // 다른 날이면 날짜 표시
                lastMessageTime = msgTime.format(DateTimeFormatter.ofPattern("M월 d일"));
            }
            
            // 사용자의 마지막 읽은 메시지 번호 조회
            Member member = memberRepository.findById(memberNo)
                    .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다: " + memberNo));
            
            Optional<ChatRoomMember> roomMemberOpt = chatRoomMemberRepository.findByChatRoomAndMember(room, member);
            if (roomMemberOpt.isPresent()) {
                Long lastReadChatNo = roomMemberOpt.get().getLastReadChatNo();
                if (lastReadChatNo != null) {
                    unreadCount = chatMessageRepository.countByChatRoomAndChatNoGreaterThan(room, lastReadChatNo);
                    System.out.println("🔍 [DEBUG] 사용자별 읽지 않은 메시지 계산: chatRoomNo=" + room.getChatRoomNo() + 
                                     ", memberNo=" + memberNo + ", lastReadChatNo=" + lastReadChatNo + ", unreadCount=" + unreadCount);
                } else {
                    // 한 번도 읽지 않았다면 모든 메시지가 읽지 않은 메시지
                    unreadCount = chatMessageRepository.countByChatRoom(room);
                    System.out.println("🔍 [DEBUG] 처음 입장 - 모든 메시지가 읽지 않음: chatRoomNo=" + room.getChatRoomNo() + 
                                     ", memberNo=" + memberNo + ", totalCount=" + unreadCount);
                }
            } else {
                // 채팅방 멤버가 아니면 읽지 않은 메시지 개수는 0
                System.out.println("🔍 [DEBUG] 채팅방 멤버가 아님: chatRoomNo=" + room.getChatRoomNo() + ", memberNo=" + memberNo);
            }
        }
        
        return ChatRoomResponseDto.builder()
                .chatRoomNo(room.getChatRoomNo())
                .chatRoomName(room.getChatRoomName())
                .chatRoomType(room.getChatRoomType())
                .projectNo(room.getProjectNo())
                .lastMessage(lastMessage)
                .lastMessageTime(lastMessageTime)
                .lastMessageSenderNo(lastMessageSenderNo)
                .lastMessageSenderName(lastMessageSenderName)
                .unreadCount(unreadCount)
                .build();
    }
}