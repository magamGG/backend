package com.kh.magamGG.domain.chat.service;

import com.kh.magamGG.domain.chat.dto.response.ChatRoomResponseDto;
import com.kh.magamGG.domain.chat.entity.ChatMessage;
import com.kh.magamGG.domain.chat.entity.ChatRoom;
import com.kh.magamGG.domain.chat.entity.ChatRoomMember;
import com.kh.magamGG.domain.chat.repository.ChatMessageRepository;
import com.kh.magamGG.domain.chat.repository.ChatRoomMemberRepository;
import com.kh.magamGG.domain.chat.repository.ChatRoomRepository;
import com.kh.magamGG.domain.attendance.entity.Attendance;
import com.kh.magamGG.domain.attendance.entity.AttendanceRequest;
import com.kh.magamGG.domain.attendance.repository.AttendanceRepository;
import com.kh.magamGG.domain.attendance.repository.AttendanceRequestRepository;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.domain.project.entity.Project;
import com.kh.magamGG.domain.project.entity.ProjectMember;
import com.kh.magamGG.domain.project.repository.ProjectMemberRepository;
import com.kh.magamGG.domain.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final AttendanceRequestRepository attendanceRequestRepository;
    private final AttendanceRepository attendanceRepository;

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
        if ("all".equals(type)) {
            Member member = memberRepository.findById(memberNo)
                    .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다: " + memberNo));
            List<ChatRoom> allRooms = new ArrayList<>();
            chatRoomRepository.findByAgencyNoAndChatRoomTypeAndChatRoomStatus(agencyNo, "ALL", "Y")
                    .ifPresent(allRooms::add);
            List<ChatRoom> projectRooms = chatRoomRepository.findProjectChatRoomsByMember(agencyNo, memberNo);
            List<ChatRoom> combinedRooms = new ArrayList<>();
            combinedRooms.addAll(allRooms);
            combinedRooms.addAll(projectRooms);
            combinedRooms.sort((a, b) -> b.getChatRoomCreatedAt().compareTo(a.getChatRoomCreatedAt()));
            return combinedRooms.stream()
                    .map(room -> convertToDtoWithUnreadCount(room, memberNo))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    /**
     * 채팅방 입장 시 멤버 자동 등록
     */
    @Override
    @Transactional
    public void joinChatRoom(Long chatRoomNo, Long memberNo) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomNo)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));
        saveChatRoomMemberIfAbsent(chatRoom, member);
    }

    /**
     * 마지막으로 읽은 메시지 업데이트
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateLastReadMessage(Long chatRoomNo, Long memberNo, Long lastChatNo) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomNo)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
        
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));
        
        // 채팅방 멤버 정보 조회
        ChatRoomMember roomMember = chatRoomMemberRepository.findByChatRoomAndMember(chatRoom, member)
                .orElseThrow(() -> new RuntimeException("채팅방 멤버가 아닙니다."));
        
        Long currentLastReadChatNo = roomMember.getLastReadChatNo();
        if (currentLastReadChatNo != null && currentLastReadChatNo.equals(lastChatNo)) {
            return false;
        }
        
        roomMember.setLastReadChatNo(lastChatNo);
        chatRoomMemberRepository.save(roomMember);
        chatRoomMemberRepository.flush();

        ChatRoomMember verifyRoomMember = chatRoomMemberRepository.findByChatRoomAndMember(chatRoom, member)
                .orElseThrow(() -> new RuntimeException("검증용 조회 실패"));
        if (!lastChatNo.equals(verifyRoomMember.getLastReadChatNo())) {
            throw new RuntimeException("DB 업데이트 실패");
        }
        return true;
    }

    /**
     * 특정 채팅방의 읽지 않은 메시지 개수 조회
     */
    @Override
    public long getUnreadCount(Long chatRoomNo, Long memberNo) {
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
            if (lastReadChatNo != null) {
                unreadCount = chatMessageRepository.countByChatRoomAndChatNoGreaterThanAndChatMessageCreatedAtGreaterThanEqual(
                        chatRoom, lastReadChatNo, roomMember.getChatRoomMemberJoinedAt());
            } else {
                unreadCount = chatMessageRepository.countByChatRoomAndChatNoGreaterThanAndChatMessageCreatedAtGreaterThanEqual(
                        chatRoom, 0L, roomMember.getChatRoomMemberJoinedAt());
            }
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

    /** 근태 신청 타입 → 금일 표시용 (휴가/재택근무/워케이션만) */
    private static String requestTypeToDisplay(String requestType) {
        if (requestType == null) return null;
        String t = requestType.trim();
        if ("연차".equals(t) || "반차".equals(t) || "반반차".equals(t) || "병가".equals(t) || "휴재".equals(t) || "휴가".equals(t)) {
            return "휴가";
        }
        if ("재택근무".equals(t) || "재택".equals(t)) return "재택근무";
        if ("워케이션".equals(t)) return "워케이션";
        return null;
    }

    /**
     * 특정 채팅방의 참여자 목록 조회 (오늘 근태 상태 todayDisplayStatus 포함)
     */
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getChatRoomMembers(Long chatRoomNo) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomNo)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다: " + chatRoomNo));
        
        List<ChatRoomMember> roomMembers = chatRoomMemberRepository.findAllByChatRoom(chatRoom);
        List<Map<String, Object>> result = new ArrayList<>();

        Long agencyNo = chatRoom.getAgencyNo();
        if (agencyNo == null && !roomMembers.isEmpty()) {
            Member first = roomMembers.get(0).getMember();
            if (first.getAgency() != null) {
                agencyNo = first.getAgency().getAgencyNo();
            }
        }

        Map<Long, String> memberStatusFromRequest = new LinkedHashMap<>();
        Map<Long, String> lastAttendanceTypeByMember = new LinkedHashMap<>();
        if (agencyNo != null) {
            LocalDate today = LocalDate.now();
            LocalDateTime todayStart = today.atStartOfDay();
            LocalDateTime todayEnd = today.atTime(LocalTime.MAX);
            List<AttendanceRequest> approvedToday;
            try {
                approvedToday = attendanceRequestRepository.findApprovedByAgencyNoAndDateBetween(agencyNo, todayStart, todayEnd);
            } catch (Exception e) {
                approvedToday = Collections.emptyList();
            }
            for (AttendanceRequest ar : approvedToday) {
                String displayType = requestTypeToDisplay(ar.getAttendanceRequestType());
                if (displayType == null) continue;
                Long memberNo = ar.getMember().getMemberNo();
                if ("휴가".equals(displayType)) memberStatusFromRequest.put(memberNo, displayType);
            }
            for (AttendanceRequest ar : approvedToday) {
                String displayType = requestTypeToDisplay(ar.getAttendanceRequestType());
                if ("재택근무".equals(displayType)) {
                    Long memberNo = ar.getMember().getMemberNo();
                    if (!memberStatusFromRequest.containsKey(memberNo)) memberStatusFromRequest.put(memberNo, displayType);
                }
            }
            for (AttendanceRequest ar : approvedToday) {
                String displayType = requestTypeToDisplay(ar.getAttendanceRequestType());
                if ("워케이션".equals(displayType)) {
                    Long memberNo = ar.getMember().getMemberNo();
                    if (!memberStatusFromRequest.containsKey(memberNo)) memberStatusFromRequest.put(memberNo, displayType);
                }
            }
            List<Attendance> todayRecords;
            try {
                todayRecords = attendanceRepository.findByAgency_AgencyNoAndDate(agencyNo, today);
            } catch (Exception e) {
                todayRecords = Collections.emptyList();
            }
            // 멤버별 오늘 마지막 근태 기록(출근/퇴근) → 작업중/작업 종료/작업 시작 전 (ORDER BY memberNo, attendanceTime DESC)
            for (Attendance a : todayRecords) {
                Long no = a.getMember().getMemberNo();
                if (!lastAttendanceTypeByMember.containsKey(no)) {
                    lastAttendanceTypeByMember.put(no, a.getAttendanceType());
                }
            }
        }
        
        final Map<Long, String> todayStatusMap = memberStatusFromRequest;
        final Map<Long, String> lastTypeMap = lastAttendanceTypeByMember;

        for (ChatRoomMember roomMember : roomMembers) {
            Member member = roomMember.getMember();
            String profileImage = member.getMemberProfileImage();
            Long memberNo = member.getMemberNo();

            // 프로젝트 상세와 동일: 휴가/재택/워케이션 → 해당 표시, 출근→작업중, 퇴근→작업 종료, 그 외/미기록→작업 시작 전
            String todayDisplayStatus = "작업 시작 전";
            if (agencyNo != null) {
                if (todayStatusMap.containsKey(memberNo)) {
                    todayDisplayStatus = todayStatusMap.get(memberNo);
                } else if (lastTypeMap.containsKey(memberNo)) {
                    String lastType = lastTypeMap.get(memberNo);
                    if ("출근".equals(lastType)) todayDisplayStatus = "작업중";
                    else if ("퇴근".equals(lastType)) todayDisplayStatus = "작업 종료";
                    else todayDisplayStatus = "작업 시작 전";
                }
            }
            
            Map<String, Object> memberInfo = new HashMap<>();
            memberInfo.put("memberNo", memberNo);
            memberInfo.put("memberName", member.getMemberName());
            memberInfo.put("memberRole", member.getMemberRole());
            memberInfo.put("memberEmail", member.getMemberEmail());
            memberInfo.put("memberPhone", member.getMemberPhone());
            memberInfo.put("memberStatus", member.getMemberStatus());
            memberInfo.put("todayDisplayStatus", todayDisplayStatus);
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
    public long getUnreadMemberCount(Long chatRoomNo, Long chatNo, Long senderMemberNo, Long requesterMemberNo) {
        return chatRoomMemberRepository.countUnreadMembersInRoomByChatNo(chatRoomNo, chatNo, senderMemberNo, requesterMemberNo);
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
            
            if (lastReadNo != null) {
                unreadCount = chatMessageRepository.countByChatRoomAndChatNoGreaterThan(room, lastReadNo);
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
                lastMessageTime = msgTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            } else {
                lastMessageTime = msgTime.format(DateTimeFormatter.ofPattern("M월 d일"));
            }
        }
        
        Long lastReadChatNo = roomMember.getLastReadChatNo();
        if (lastReadChatNo != null) {
            unreadCount = chatMessageRepository.countByChatRoomAndChatNoGreaterThanAndChatMessageCreatedAtGreaterThanEqual(
                    room, lastReadChatNo, roomMember.getChatRoomMemberJoinedAt());
        } else {
            unreadCount = chatMessageRepository.countByChatRoomAndChatNoGreaterThanAndChatMessageCreatedAtGreaterThanEqual(
                    room, 0L, roomMember.getChatRoomMemberJoinedAt());
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
                    lastMessageTime = msgTime.format(DateTimeFormatter.ofPattern("HH:mm"));
                } else {
                    lastMessageTime = msgTime.format(DateTimeFormatter.ofPattern("M월 d일"));
                }
            }
            
            Long lastReadChatNo = roomMember.getLastReadChatNo();
            if (lastReadChatNo != null) {
                unreadCount = chatMessageRepository.countByChatRoomAndChatNoGreaterThanAndChatMessageCreatedAtGreaterThanEqual(
                        room, lastReadChatNo, roomMember.getChatRoomMemberJoinedAt());
            } else {
                unreadCount = chatMessageRepository.countByChatRoomAndChatNoGreaterThanAndChatMessageCreatedAtGreaterThanEqual(
                        room, 0L, roomMember.getChatRoomMemberJoinedAt());
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