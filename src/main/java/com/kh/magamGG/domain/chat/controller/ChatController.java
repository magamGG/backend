package com.kh.magamGG.domain.chat.controller;

import com.kh.magamGG.domain.chat.dto.request.ChatMessageRequestDto;
import com.kh.magamGG.domain.chat.dto.response.ChatMessageResponseDto;
import com.kh.magamGG.domain.chat.dto.response.ChatRoomResponseDto;
import com.kh.magamGG.domain.chat.service.ChatMessageService;
import com.kh.magamGG.domain.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageService chatMessageService;
    private final ChatRoomService chatRoomService;
    private final SimpMessageSendingOperations messagingTemplate;

    /**
     * 1. 실시간 메시지 전송 (WebSocket)
     * 클라이언트가 /app/chat/message로 보내면 이 메서드가 실행됨
     */
    @MessageMapping("/chat/message")
    public void message(ChatMessageRequestDto message) {
        // DB에 메시지 저장 (실명 포함된 ResponseDto 반환)
        ChatMessageResponseDto responseMessage = chatMessageService.saveMessage(message);

        // 해당 방을 구독 중인(/topic/room/{roomNo}) 유저들에게 메시지 브로드캐스트
        messagingTemplate.convertAndSend("/topic/room/" + responseMessage.getChatRoomNo(), responseMessage);
    }

    /**
     * 2. 에이전시별 채팅방 목록 조회 (HTTP GET)
     * type이 "all"이면 같은 agencyNo의 모든 멤버가 포함된 채팅방들을 반환
     * 단, PROJECT 타입 채팅방은 해당 프로젝트에 참여한 멤버만 볼 수 있음
     */
    @GetMapping("/api/chat/rooms/agency/{agencyNo}")
    public ResponseEntity<List<ChatRoomResponseDto>> getChatRoomsByAgency(
            @PathVariable Long agencyNo,
            @RequestParam(defaultValue = "all") String type,
            @RequestHeader(value = "X-Member-No", required = false) Long memberNo) {
        
        System.out.println("=== 채팅방 목록 조회 API 호출 ===");
        System.out.println("agencyNo: " + agencyNo);
        System.out.println("type: " + type);
        System.out.println("memberNo: " + memberNo);
        
        // 테스트용: memberNo가 null이면 기본값 1 사용
        if (memberNo == null) {
            memberNo = 1L;
            System.out.println("memberNo가 null이어서 기본값 1로 설정");
        }
        
        try {
            List<ChatRoomResponseDto> rooms = chatRoomService.getChatRoomsByAgency(agencyNo, type, memberNo);
            System.out.println("조회된 채팅방 개수: " + (rooms != null ? rooms.size() : 0));
            return ResponseEntity.ok(rooms);
        } catch (Exception e) {
            System.out.println("채팅방 조회 중 에러 발생: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 3. 내 채팅방 목록 조회 (HTTP GET) - 기존 메서드 유지
     */
    @GetMapping("/api/chat/rooms/{memberNo}")
    public ResponseEntity<List<ChatRoomResponseDto>> getMyRooms(@PathVariable Long memberNo) {
        List<ChatRoomResponseDto> rooms = chatRoomService.getMyChatRooms(memberNo);
        return ResponseEntity.ok(rooms);
    }

    /**
     * 4. 채팅방 입장 (HTTP POST)
     * 사용자를 채팅방 멤버로 등록
     */
    @PostMapping("/api/chat/rooms/{chatRoomNo}/join")
    public ResponseEntity<Void> joinChatRoom(
            @PathVariable Long chatRoomNo,
            @RequestHeader("X-Member-No") Long memberNo) {
        chatRoomService.joinChatRoom(chatRoomNo, memberNo);
        return ResponseEntity.ok().build();
    }

    /**
     * 5. 특정 방의 과거 대화 내역 조회 (HTTP GET, 페이징) - 멤버 입장 시간 이후만
     * 예: /api/chat/rooms/1/messages?page=0&size=20
     */
    @GetMapping("/api/chat/rooms/{chatRoomNo}/messages")
    public ResponseEntity<Slice<ChatMessageResponseDto>> getChatHistory(
            @PathVariable Long chatRoomNo,
            @RequestHeader("X-Member-No") Long memberNo,
            @PageableDefault(size = 20) Pageable pageable) {

        Slice<ChatMessageResponseDto> history = chatMessageService.getChatHistory(chatRoomNo, memberNo, pageable);
        return ResponseEntity.ok(history);
    }

    /**
     * 6. 마지막으로 읽은 메시지 업데이트 (HTTP PUT)
     */
    @PutMapping("/api/chat/rooms/{chatRoomNo}/read")
    public ResponseEntity<Void> updateLastReadMessage(
            @PathVariable Long chatRoomNo,
            @RequestParam Long lastChatNo,
            @RequestHeader("X-Member-No") Long memberNo) {
        boolean updated = chatRoomService.updateLastReadMessage(chatRoomNo, memberNo, lastChatNo);
        // 읽음 실시간 반영: 실제로 DB가 갱신된 경우에만 해당 방 구독자에게 브로드캐스트
        if (updated) {
            Map<String, Object> readUpdate = new HashMap<>();
            readUpdate.put("type", "READ_UPDATE");
            readUpdate.put("memberNo", memberNo);
            readUpdate.put("lastChatNo", lastChatNo);
            messagingTemplate.convertAndSend("/topic/room/" + chatRoomNo, readUpdate);
            System.out.println("[READ_UPDATE] 브로드캐스트 room=" + chatRoomNo + " memberNo=" + memberNo + " lastChatNo=" + lastChatNo);
        }
        return ResponseEntity.ok().build();
    }

    /**
     * 6-1. 특정 메시지를 읽지 않은 참여 중인 멤버 수 (카카오톡 스타일)
     * senderMemberNo: 발신자 제외 시 전달 (제외하지 않으면 생략)
     */
    @GetMapping("/api/chat/rooms/{chatRoomNo}/messages/{chatNo}/read-status")
    public ResponseEntity<Map<String, Object>> getMessageReadStatus(
            @PathVariable Long chatRoomNo,
            @PathVariable Long chatNo,
            @RequestParam(required = false) Long senderMemberNo) {
        long unreadMemberCount = chatRoomService.getUnreadMemberCount(chatRoomNo, chatNo, senderMemberNo);
        Map<String, Object> body = new HashMap<>();
        body.put("unreadMemberCount", unreadMemberCount);
        return ResponseEntity.ok(body);
    }

    /**
     * 7. 특정 채팅방의 읽지 않은 메시지 개수 조회 (HTTP GET)
     */
    @GetMapping("/api/chat/rooms/{chatRoomNo}/unread-count")
    public ResponseEntity<Long> getUnreadCount(
            @PathVariable Long chatRoomNo,
            @RequestHeader("X-Member-No") Long memberNo) {
        long unreadCount = chatRoomService.getUnreadCount(chatRoomNo, memberNo);
        return ResponseEntity.ok(unreadCount);
    }

    /**
     * 8. 특정 채팅방의 참여자 목록 조회 (HTTP GET)
     */
    @GetMapping("/api/chat/rooms/{chatRoomNo}/members")
    public ResponseEntity<List<Map<String, Object>>> getChatRoomMembers(
            @PathVariable Long chatRoomNo) {
        List<Map<String, Object>> members = chatRoomService.getChatRoomMembers(chatRoomNo);
        return ResponseEntity.ok(members);
    }

    /**
     * 9. 채팅 버튼 클릭 시 자동으로 채팅방 생성 및 참여자 초대 (HTTP POST)
     */
    @PostMapping("/api/chat/ensure-rooms")
    public ResponseEntity<Void> ensureChatRooms(
            @RequestHeader("X-Member-No") Long memberNo) {
        chatRoomService.ensureChatRoomsAndInviteMembers(memberNo);
        return ResponseEntity.ok().build();
    }

    /**
     * 11. 사용자의 마지막 읽은 메시지 번호 조회 (HTTP GET)
     */
    @GetMapping("/api/chat/rooms/{chatRoomNo}/last-read")
    public ResponseEntity<Map<String, Object>> getLastReadMessage(
            @PathVariable Long chatRoomNo,
            @RequestHeader("X-Member-No") Long memberNo) {
        Long lastReadChatNo = chatRoomService.getLastReadChatNo(chatRoomNo, memberNo);
        Map<String, Object> response = new HashMap<>();
        response.put("lastReadChatNo", lastReadChatNo);
        return ResponseEntity.ok(response);
    }

    /**
     * 12. 채팅 파일 업로드 (HTTP POST)
     */
    @PostMapping("/api/chat/rooms/{chatRoomNo}/upload")
    public ResponseEntity<Map<String, Object>> uploadChatFile(
            @PathVariable Long chatRoomNo,
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-Member-No") Long memberNo) {

        try {
            // 파일 크기 제한 (10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "파일 크기는 10MB 이하만 업로드 가능합니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 파일 업로드 처리 및 메시지 저장
            ChatMessageResponseDto savedMessage = chatMessageService.uploadFileAndSaveMessage(file, chatRoomNo, memberNo);

            // WebSocket을 통해 실시간으로 메시지 전송
            messagingTemplate.convertAndSend("/topic/room/" + chatRoomNo, savedMessage);

            Map<String, Object> response = new HashMap<>();
            response.put("fileUrl", savedMessage.getAttachmentUrl());
            response.put("fileName", file.getOriginalFilename());
            response.put("fileSize", file.getSize());
            response.put("message", savedMessage);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "파일 업로드에 실패했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 13. 채팅 파일 다운로드 (HTTP GET)
     */
    @GetMapping("/api/chat/rooms/{chatRoomNo}/files/download/{fileName}")
    public ResponseEntity<org.springframework.core.io.Resource> downloadChatFile(
            @PathVariable Long chatRoomNo,
            @PathVariable String fileName,
            @RequestHeader(value = "X-Member-No", required = false) Long memberNo) {

        System.out.println("=== 채팅 파일 다운로드 API 호출 ===");
        System.out.println("chatRoomNo: " + chatRoomNo);
        System.out.println("fileName: " + fileName);
        System.out.println("memberNo: " + memberNo);

        try {
            return chatMessageService.downloadChatFile(fileName);
        } catch (Exception e) {
            System.out.println("다운로드 에러: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
