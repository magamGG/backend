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
import com.kh.magamGG.domain.project.entity.Project;
import com.kh.magamGG.domain.project.entity.ProjectMember;
import com.kh.magamGG.domain.project.repository.ProjectMemberRepository;
import com.kh.magamGG.domain.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final ProjectRepository projectRepository;

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
                    // ChatRoomMember 객체를 직접 전달하여 join_at 시점을 고려한 계산
                    return convertToDto(room, mapping);
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
        System.out.println("=== ChatRoomService.getChatRoomsByAgency 호출 ===");
        System.out.println("agencyNo: " + agencyNo + ", type: " + type + ", memberNo: " + memberNo);
        
        if ("all".equals(type)) {
            try {
                System.out.println("회원 조회 시작: memberNo = " + memberNo);
                Member member = memberRepository.findById(memberNo)
                        .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다: " + memberNo));
                System.out.println("회원 조회 성공: " + member.getMemberName());
                
                // 1. 에이전시 전체 채팅방 조회 (ALL 타입)
                System.out.println("에이전시 전체 채팅방 조회 시작");
                List<ChatRoom> allRooms = new ArrayList<>();
                chatRoomRepository.findByAgencyNoAndChatRoomTypeAndChatRoomStatus(agencyNo, "ALL", "Y")
                        .ifPresent(room -> {
                            System.out.println("전체 채팅방 발견: " + room.getChatRoomName());
                            allRooms.add(room);
                        });
                System.out.println("전체 채팅방 개수: " + allRooms.size());
                
                // 2. 내가 참여한 프로젝트의 채팅방들 조회 (PROJECT 타입)
                System.out.println("프로젝트 채팅방 조회 시작");
                List<ChatRoom> projectRooms = chatRoomRepository.findProjectChatRoomsByMember(agencyNo, memberNo);
                System.out.println("프로젝트 채팅방 개수: " + projectRooms.size());
                
                // 3. 두 리스트를 합치고 생성일 역순으로 정렬
                List<ChatRoom> combinedRooms = new ArrayList<>();
                combinedRooms.addAll(allRooms);
                combinedRooms.addAll(projectRooms);
                
                combinedRooms.sort((a, b) -> b.getChatRoomCreatedAt().compareTo(a.getChatRoomCreatedAt()));
                
                System.out.println("최종 채팅방 개수: " + combinedRooms.size());
                
                List<ChatRoomResponseDto> result = combinedRooms.stream()
                        .map(room -> convertToDtoWithUnreadCount(room, memberNo))
                        .collect(Collectors.toList());
                
                System.out.println("DTO 변환 완료, 반환할 채팅방 개수: " + result.size());
                return result;
                
            } catch (Exception e) {
                System.out.println("getChatRoomsByAgency 에러: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
        } else {
            System.out.println("type이 'all'이 아님: " + type);
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
    public boolean updateLastReadMessage(Long chatRoomNo, Long memberNo, Long lastChatNo) {
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
            
            // 현재 unread count 확인 (입장 시간 이후만)
            long currentUnreadCount = chatMessageRepository.countByChatRoomAndChatNoGreaterThanAndChatMessageCreatedAtGreaterThanEqual(
                    chatRoom, currentLastReadChatNo, roomMember.getChatRoomMemberJoinedAt());
            System.out.println("🔍 [DEBUG] 현재 unread count (입장 시간 이후): " + currentUnreadCount);
            return false;
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
        
        // 업데이트 후 unread count 재계산 (입장 시간 이후만)
        long newUnreadCount = chatMessageRepository.countByChatRoomAndChatNoGreaterThanAndChatMessageCreatedAtGreaterThanEqual(
                chatRoom, lastChatNo, roomMember.getChatRoomMemberJoinedAt());
        System.out.println("🔍 [DEBUG] 업데이트 후 새로운 unread count (입장 시간 이후): " + newUnreadCount);
        
        System.out.println("✅ [DEBUG] 마지막 읽은 메시지 업데이트 완료: chatRoomNo=" + chatRoomNo + 
                          ", memberNo=" + memberNo + ", 새로운 lastChatNo=" + lastChatNo);
        return true;
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
            ChatRoomMember roomMember = roomMemberOpt.get();
            Long lastReadChatNo = roomMember.getLastReadChatNo();
            long unreadCount;
            
            System.out.println("🔍 [DEBUG] 현재 lastReadChatNo: " + lastReadChatNo);
            System.out.println("🔍 [DEBUG] 멤버 입장 시간: " + roomMember.getChatRoomMemberJoinedAt());
            
            if (lastReadChatNo != null) {
                // 멤버 입장 시간 이후이면서 마지막 읽은 메시지 이후의 메시지 개수
                unreadCount = chatMessageRepository.countByChatRoomAndChatNoGreaterThanAndChatMessageCreatedAtGreaterThanEqual(
                        chatRoom, lastReadChatNo, roomMember.getChatRoomMemberJoinedAt());
                System.out.println("🔍 [DEBUG] countByChatRoomAndChatNoGreaterThanAndChatMessageCreatedAtGreaterThanEqual 쿼리 결과: " + unreadCount);
                System.out.println("🔍 [DEBUG] 쿼리 조건: chatRoom=" + chatRoom.getChatRoomNo() + 
                                 ", lastReadChatNo > " + lastReadChatNo + 
                                 ", createdAt >= " + roomMember.getChatRoomMemberJoinedAt());
            } else {
                // 한 번도 읽지 않았다면 멤버 입장 시간 이후의 모든 메시지가 읽지 않은 메시지
                unreadCount = chatMessageRepository.countByChatRoomAndChatNoGreaterThanAndChatMessageCreatedAtGreaterThanEqual(
                        chatRoom, 0L, roomMember.getChatRoomMemberJoinedAt());
                System.out.println("🔍 [DEBUG] 처음 입장 시 멤버 입장 이후 메시지 개수: " + unreadCount);
            }
            
            System.out.println("🔍 [DEBUG] getUnreadCount 결과: chatRoomNo=" + chatRoomNo + 
                             ", memberNo=" + memberNo + ", lastReadChatNo=" + lastReadChatNo + 
                             ", unreadCount=" + unreadCount);
            return unreadCount;
        } else {
            // 채팅방 멤버가 아니면 읽지 않은 메시지 개수는 0
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

    /**
     * 특정 채팅방의 참여자 목록 조회
     */
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getChatRoomMembers(Long chatRoomNo) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomNo)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다: " + chatRoomNo));
        
        List<ChatRoomMember> roomMembers = chatRoomMemberRepository.findAllByChatRoom(chatRoom);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (ChatRoomMember roomMember : roomMembers) {
            Member member = roomMember.getMember();
            String profileImage = member.getMemberProfileImage();
            
            Map<String, Object> memberInfo = new HashMap<>();
            memberInfo.put("memberNo", member.getMemberNo());
            memberInfo.put("memberName", member.getMemberName());
            memberInfo.put("memberRole", member.getMemberRole());
            memberInfo.put("profileImage", profileImage);
            memberInfo.put("joinedAt", roomMember.getChatRoomMemberJoinedAt());
            
            result.add(memberInfo);
        }
        
        return result;
    }

    /**
     * 채팅 버튼 클릭 시 자동으로 채팅방 생성 및 참여자 초대
     * 마지막 chatRoom을 만들 때, chat Room의 type이 all이면 전체 채팅방을 project면 project를 조회해서 채팅방을 만든다.
     */
    @Override
    @Transactional
    public void ensureChatRoomsAndInviteMembers(Long memberNo) {
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다: " + memberNo));
        
        Long agencyNo = member.getAgency() != null ? member.getAgency().getAgencyNo() : null;
        
        if (agencyNo == null) {
            return;
        }
        
        // 1. 에이전시 전체 채팅방 생성 및 참여 (type = "ALL")
        ensureAgencyChatRoom(member, agencyNo);
        
        // 2. 참여 중인 프로젝트 채팅방들 생성 및 참여 (type = "PROJECT")
        ensureProjectChatRooms(member, agencyNo);
    }

    /**
     * 에이전시 전체 채팅방 생성 및 참여
     */
    private void ensureAgencyChatRoom(Member member, Long agencyNo) {
        // 에이전시 전체 채팅방 조회 또는 생성
        ChatRoom agencyRoom = chatRoomRepository.findByAgencyNoAndChatRoomTypeAndChatRoomStatus(agencyNo, "ALL", "Y")
                .orElseGet(() -> createAgencyTotalRoom(agencyNo));
        
        // 해당 에이전시의 모든 활성 멤버들을 채팅방에 추가
        List<Member> agencyMembers = memberRepository.findByAgency_AgencyNoAndMemberStatusActive(agencyNo);
        
        for (Member agencyMember : agencyMembers) {
            saveChatRoomMemberIfAbsent(agencyRoom, agencyMember);
        }
    }

    /**
     * 참여 중인 프로젝트 채팅방들 생성 및 참여
     */
    private void ensureProjectChatRooms(Member member, Long agencyNo) {
        // 해당 멤버가 참여 중인 프로젝트들 조회
        List<ProjectMember> projectMembers = projectMemberRepository.findByMember_MemberNo(member.getMemberNo());
        
        for (ProjectMember projectMember : projectMembers) {
            Project project = projectMember.getProject();
            Long projectNo = project.getProjectNo();
            String projectName = project.getProjectName();
            
            // 프로젝트 채팅방 조회 또는 생성
            ChatRoom projectRoom = chatRoomRepository.findByAgencyNoAndProjectNoAndChatRoomTypeAndChatRoomStatus(
                            agencyNo, projectNo, "PROJECT", "Y")
                    .orElseGet(() -> createNewProjectRoom(agencyNo, projectNo, projectName));
            
            // 해당 프로젝트의 모든 멤버들을 채팅방에 추가
            List<ProjectMember> allProjectMembers = projectMemberRepository.findByProject_ProjectNo(projectNo);
            
            for (ProjectMember pm : allProjectMembers) {
                Member projectMemberEntity = pm.getMember();
                saveChatRoomMemberIfAbsent(projectRoom, projectMemberEntity);
            }
        }
    }

    /**
     * 간단한 채팅방 멤버 로그 출력 (프로필 정보)
     */
    @Override
    @Transactional(readOnly = true)
    public void logChatRoomMembers(Long chatRoomNo) {
        try {
            ChatRoom chatRoom = chatRoomRepository.findById(chatRoomNo)
                    .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다: " + chatRoomNo));
            
            List<ChatRoomMember> roomMembers = chatRoomMemberRepository.findAllByChatRoom(chatRoom);
            
            for (ChatRoomMember roomMember : roomMembers) {
                Member member = roomMember.getMember();
                // 로그 제거됨 - 필요시 디버깅 목적으로만 사용
            }
            
        } catch (Exception e) {
            // 에러 발생 시에만 로그 출력
            System.err.println("채팅방 멤버 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 사용자의 마지막 읽은 메시지 번호 조회
     */
    @Override
    @Transactional(readOnly = true)
    public Long getLastReadChatNo(Long chatRoomNo, Long memberNo) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomNo)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다: " + chatRoomNo));
        
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다: " + memberNo));
        
        Optional<ChatRoomMember> roomMemberOpt = chatRoomMemberRepository.findByChatRoomAndMember(chatRoom, member);
        
        if (roomMemberOpt.isPresent()) {
            return roomMemberOpt.get().getLastReadChatNo();
        } else {
            // 채팅방 멤버가 아니면 null 반환
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadMemberCount(Long chatRoomNo, Long chatNo, Long senderMemberNo) {
        return chatRoomMemberRepository.countUnreadMembersInRoomByChatNo(chatRoomNo, chatNo, senderMemberNo);
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

    // DTO 변환 로직 개선 - 실제 마지막 메시지와 시간 포함 (Deprecated: ChatRoomMember 버전 사용 권장)
    @Deprecated
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
            // 주의: 이 메서드는 join_at 정보가 없어서 정확하지 않을 수 있음
            if (lastReadNo != null) {
                unreadCount = chatMessageRepository.countByChatRoomAndChatNoGreaterThan(room, lastReadNo);
                System.out.println("⚠️ [DEPRECATED] 읽지 않은 메시지 계산 (join_at 미고려): chatRoomNo=" + room.getChatRoomNo() + 
                                 ", lastReadNo=" + lastReadNo + ", unreadCount=" + unreadCount);
            } else {
                System.out.println("⚠️ [DEPRECATED] lastReadNo가 null이므로 unreadCount 계산 안 함: chatRoomNo=" + room.getChatRoomNo());
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

    // ChatRoomMember 객체를 받는 오버로드된 convertToDto 메서드 (join_at 시점 고려)
    private ChatRoomResponseDto convertToDto(ChatRoom room, ChatRoomMember roomMember) {
        String lastMessage = "";
        String lastMessageTime = "";
        Long lastMessageSenderNo = null;
        String lastMessageSenderName = "";
        long unreadCount = 0;
        
        // 멤버 입장 시간 이후의 마지막 메시지만 조회
        Optional<ChatMessage> lastMessageOpt = chatMessageRepository
                .findFirstByChatRoomAndChatMessageCreatedAtGreaterThanEqualOrderByChatMessageCreatedAtDesc(
                        room, roomMember.getChatRoomMemberJoinedAt());
        
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
            
            System.out.println("🔍 [DEBUG] 입장 시간 이후 마지막 메시지: chatRoomNo=" + room.getChatRoomNo() + 
                             ", memberNo=" + roomMember.getMember().getMemberNo() + ", joinedAt=" + roomMember.getChatRoomMemberJoinedAt() + 
                             ", lastMessage=" + lastMessage + ", lastMessageTime=" + lastMessageTime);
        } else {
            System.out.println("🔍 [DEBUG] 입장 시간 이후 메시지 없음: chatRoomNo=" + room.getChatRoomNo() + 
                             ", memberNo=" + roomMember.getMember().getMemberNo() + ", joinedAt=" + roomMember.getChatRoomMemberJoinedAt());
        }
        
        // 읽지 않은 메시지 개수 계산 (join_at 시점 고려)
        Long lastReadChatNo = roomMember.getLastReadChatNo();
        if (lastReadChatNo != null) {
            // 멤버 입장 시간 이후이면서 마지막 읽은 메시지 이후의 메시지 개수
            unreadCount = chatMessageRepository.countByChatRoomAndChatNoGreaterThanAndChatMessageCreatedAtGreaterThanEqual(
                    room, lastReadChatNo, roomMember.getChatRoomMemberJoinedAt());
            System.out.println("🔍 [DEBUG] 읽지 않은 메시지 계산 (입장 시간 이후): chatRoomNo=" + room.getChatRoomNo() + 
                             ", memberNo=" + roomMember.getMember().getMemberNo() + ", lastReadChatNo=" + lastReadChatNo + 
                             ", joinedAt=" + roomMember.getChatRoomMemberJoinedAt() + ", unreadCount=" + unreadCount);
        } else {
            // 한 번도 읽지 않았다면 멤버 입장 시간 이후의 모든 메시지가 읽지 않은 메시지
            unreadCount = chatMessageRepository.countByChatRoomAndChatNoGreaterThanAndChatMessageCreatedAtGreaterThanEqual(
                    room, 0L, roomMember.getChatRoomMemberJoinedAt());
            System.out.println("🔍 [DEBUG] 처음 입장 - 입장 시간 이후 메시지만 계산: chatRoomNo=" + room.getChatRoomNo() + 
                             ", memberNo=" + roomMember.getMember().getMemberNo() + ", joinedAt=" + roomMember.getChatRoomMemberJoinedAt() + ", totalCount=" + unreadCount);
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
        // 사용자의 채팅방 멤버 정보 먼저 조회 (join_at 시점 확인용)
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다: " + memberNo));
        
        Optional<ChatRoomMember> roomMemberOpt = chatRoomMemberRepository.findByChatRoomAndMember(room, member);
        
        String lastMessage = "";
        String lastMessageTime = "";
        Long lastMessageSenderNo = null;
        String lastMessageSenderName = "";
        long unreadCount = 0;
        
        if (roomMemberOpt.isPresent()) {
            ChatRoomMember roomMember = roomMemberOpt.get();
            
            // 멤버 입장 시간 이후의 마지막 메시지만 조회
            Optional<ChatMessage> lastMessageOpt = chatMessageRepository
                    .findFirstByChatRoomAndChatMessageCreatedAtGreaterThanEqualOrderByChatMessageCreatedAtDesc(
                            room, roomMember.getChatRoomMemberJoinedAt());
            
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
                
                System.out.println("🔍 [DEBUG] 입장 시간 이후 마지막 메시지: chatRoomNo=" + room.getChatRoomNo() + 
                                 ", memberNo=" + memberNo + ", joinedAt=" + roomMember.getChatRoomMemberJoinedAt() + 
                                 ", lastMessage=" + lastMessage + ", lastMessageTime=" + lastMessageTime);
            } else {
                System.out.println("🔍 [DEBUG] 입장 시간 이후 메시지 없음: chatRoomNo=" + room.getChatRoomNo() + 
                                 ", memberNo=" + memberNo + ", joinedAt=" + roomMember.getChatRoomMemberJoinedAt());
            }
            
            // 읽지 않은 메시지 개수 계산 (입장 시간 이후만)
            Long lastReadChatNo = roomMember.getLastReadChatNo();
            if (lastReadChatNo != null) {
                // 멤버 입장 시간 이후이면서 마지막 읽은 메시지 이후의 메시지 개수
                unreadCount = chatMessageRepository.countByChatRoomAndChatNoGreaterThanAndChatMessageCreatedAtGreaterThanEqual(
                        room, lastReadChatNo, roomMember.getChatRoomMemberJoinedAt());
                System.out.println("🔍 [DEBUG] 사용자별 읽지 않은 메시지 계산 (입장 시간 이후): chatRoomNo=" + room.getChatRoomNo() + 
                                 ", memberNo=" + memberNo + ", lastReadChatNo=" + lastReadChatNo + 
                                 ", joinedAt=" + roomMember.getChatRoomMemberJoinedAt() + ", unreadCount=" + unreadCount);
            } else {
                // 한 번도 읽지 않았다면 멤버 입장 시간 이후의 모든 메시지가 읽지 않은 메시지
                unreadCount = chatMessageRepository.countByChatRoomAndChatNoGreaterThanAndChatMessageCreatedAtGreaterThanEqual(
                        room, 0L, roomMember.getChatRoomMemberJoinedAt());
                System.out.println("🔍 [DEBUG] 처음 입장 - 입장 시간 이후 메시지만 계산: chatRoomNo=" + room.getChatRoomNo() + 
                                 ", memberNo=" + memberNo + ", joinedAt=" + roomMember.getChatRoomMemberJoinedAt() + ", totalCount=" + unreadCount);
            }
        } else {
            // 채팅방 멤버가 아니면 읽지 않은 메시지 개수는 0
            System.out.println("🔍 [DEBUG] 채팅방 멤버가 아님: chatRoomNo=" + room.getChatRoomNo() + ", memberNo=" + memberNo);
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