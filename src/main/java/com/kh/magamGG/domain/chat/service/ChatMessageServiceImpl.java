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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본 조회용, 저장 메서드만 @Transactional 따로 부여
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final MemberRepository memberRepository;
    
    @Value("${file.upload-dir:uploads}")
    private String uploadPath;

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
    /**
     * 채팅 내역 조회 (무한 스크롤 최적화) - 멤버 입장 시간 이후 메시지만 조회
     */
    @Override
    public Slice<ChatMessageResponseDto> getChatHistory(Long chatRoomNo, Long memberNo, Pageable pageable) {
        ChatRoom room = chatRoomRepository.findById(chatRoomNo)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));

        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        // 멤버의 채팅방 입장 정보 조회
        ChatRoomMember roomMember = chatRoomMemberRepository.findByChatRoomAndMember(room, member)
                .orElseThrow(() -> new RuntimeException("해당 방의 참여 멤버가 아닙니다."));

        // 멤버 입장 시간 이후의 메시지만 조회
        Slice<ChatMessage> messages = chatMessageRepository
                .findAllByChatRoomAndChatStatusAndChatMessageCreatedAtGreaterThanEqualOrderByChatMessageCreatedAtDesc(
                        room, "Y", roomMember.getChatRoomMemberJoinedAt(), pageable);

        // 엔티티 Slice를 DTO Slice로 변환하여 반환
        return messages.map(ChatMessageResponseDto::from);
    }


    /**
     * 안 읽은 메시지 수 카운트 - 멤버 입장 시간 이후만
     */
    @Override
    public long getUnreadCount(Long chatRoomNo, Long memberNo) {
        ChatRoom room = chatRoomRepository.findById(chatRoomNo).orElseThrow();
        Member member = memberRepository.findById(memberNo).orElseThrow();

        // 참여 정보 조회
        ChatRoomMember roomMember = chatRoomMemberRepository.findByChatRoomAndMember(room, member)
                .orElseThrow(() -> new RuntimeException("해당 방의 참여 멤버가 아닙니다."));

        Long lastReadNo = roomMember.getLastReadChatNo();

        // 마지막으로 읽은 메시지 ID 이후이면서 멤버 입장 시간 이후의 메시지 개수를 리턴
        return chatMessageRepository.countByChatRoomAndChatNoGreaterThanAndChatMessageCreatedAtGreaterThanEqual(
                room, lastReadNo != null ? lastReadNo : 0L, roomMember.getChatRoomMemberJoinedAt());
    }

    /**
     * 채팅 파일 업로드
     */
    @Override
    @Transactional
    public String uploadFile(MultipartFile file, Long chatRoomNo, Long memberNo) throws Exception {
        // 채팅방과 멤버 검증
        ChatRoom room = chatRoomRepository.findById(chatRoomNo)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
        
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        // 설정된 업로드 디렉토리 아래 chat 폴더에 저장
        Path uploadBaseDir = Paths.get(uploadPath).toAbsolutePath();
        Path chatUploadDir = uploadBaseDir.resolve("chat");
        
        // 디렉토리가 존재하지 않으면 생성
        if (!Files.exists(chatUploadDir)) {
            try {
                Files.createDirectories(chatUploadDir);
            } catch (IOException e) {
                throw new RuntimeException("채팅 업로드 디렉토리 생성에 실패했습니다: " + e.getMessage(), e);
            }
        }

        // 파일명 생성 (UUID + 원본 확장자)
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String savedFilename = UUID.randomUUID().toString() + extension;

        // 파일 저장
        Path filePath = chatUploadDir.resolve(savedFilename);
        
        try {
            // 파일을 지정된 경로에 저장
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("파일 저장에 실패했습니다: " + e.getMessage(), e);
        }

        // 파일 타입 결정 (이미지 파일인지 확인)
        String messageType = isImageFile(extension) ? "IMAGE" : "FILE";

        // 파일 정보를 채팅 메시지로 저장
        String fileMessage = String.format("%s (%s)", 
            originalFilename, 
            formatFileSize(file.getSize()));

        // 파일 URL 생성
        String fileUrl = "/uploads/chat/" + savedFilename;

        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .member(member)
                .chatMessage(fileMessage)
                .chatMessageType(messageType)  // IMAGE 또는 FILE
                .attachmentUrl(fileUrl)  // 파일 URL을 attachment_url에 저장
                .chatStatus("Y")
                .chatMessageCreatedAt(LocalDateTime.now())
                .build();

        try {
            chatMessageRepository.save(message);
        } catch (Exception e) {
            // 메시지 저장 실패 시 업로드된 파일 삭제
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException deleteException) {
                // 파일 삭제 실패는 로그만 남기고 원본 예외를 던짐
            }
            throw new RuntimeException("채팅 메시지 저장에 실패했습니다: " + e.getMessage(), e);
        }

        // 파일 URL 반환 (상대 경로)
        return fileUrl;
    }

    /**
     * 파일 업로드 및 메시지 저장 (WebSocket 전송용)
     */
    @Override
    @Transactional
    public ChatMessageResponseDto uploadFileAndSaveMessage(MultipartFile file, Long chatRoomNo, Long memberNo) throws Exception {
        // 채팅방과 멤버 검증
        ChatRoom room = chatRoomRepository.findById(chatRoomNo)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
        
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        // 설정된 업로드 디렉토리 아래 chat 폴더에 저장
        Path uploadBaseDir = Paths.get(uploadPath).toAbsolutePath();
        Path chatUploadDir = uploadBaseDir.resolve("chat");
        
        // 디렉토리가 존재하지 않으면 생성
        if (!Files.exists(chatUploadDir)) {
            try {
                Files.createDirectories(chatUploadDir);
            } catch (IOException e) {
                throw new RuntimeException("채팅 업로드 디렉토리 생성에 실패했습니다: " + e.getMessage(), e);
            }
        }

        // 파일명 생성 (UUID + 원본 확장자)
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String savedFilename = UUID.randomUUID().toString() + extension;

        // 파일 저장
        Path filePath = chatUploadDir.resolve(savedFilename);
        
        try {
            // 파일을 지정된 경로에 저장
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("파일 저장에 실패했습니다: " + e.getMessage(), e);
        }

        // 파일 정보를 채팅 메시지로 저장
        String fileMessage = String.format("%s (%s)",
            originalFilename, 
            formatFileSize(file.getSize()));

        // 파일 타입 결정 (이미지 파일인지 확인)
        String messageType = isImageFile(extension) ? "IMAGE" : "FILE";

        // 파일 URL 생성
        String fileUrl = "/uploads/chat/" + savedFilename;

        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .member(member)
                .chatMessage(fileMessage)
                .chatMessageType(messageType)  // IMAGE 또는 FILE
                .attachmentUrl(fileUrl)  // 파일 URL을 attachment_url에 저장
                .chatStatus("Y")
                .chatMessageCreatedAt(LocalDateTime.now())
                .build();

        try {
            ChatMessage savedMessage = chatMessageRepository.save(message);
            // ResponseDto로 변환하여 반환
            return ChatMessageResponseDto.from(savedMessage);
        } catch (Exception e) {
            // 메시지 저장 실패 시 업로드된 파일 삭제
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException deleteException) {
                // 파일 삭제 실패는 로그만 남기고 원본 예외를 던짐
            }
            throw new RuntimeException("채팅 메시지 저장에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 파일 크기를 읽기 쉬운 형태로 변환
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        }
    }

    /**
     * 파일 확장자로 이미지 파일인지 확인
     */
    private boolean isImageFile(String extension) {
        if (extension == null || extension.isEmpty()) {
            return false;
        }
        String ext = extension.toLowerCase();
        return ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png") || 
               ext.equals(".gif") || ext.equals(".webp") || ext.equals(".bmp");
    }

    /**
     * 채팅 파일 다운로드
     */
    @Override
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> downloadChatFile(String fileName) throws Exception {
        try {
            // 설정된 업로드 디렉토리 아래 chat 폴더에서 파일 찾기
            Path uploadBaseDir = Paths.get(uploadPath).toAbsolutePath();
            Path chatUploadDir = uploadBaseDir.resolve("chat");
            Path filePath = chatUploadDir.resolve(fileName);

            System.out.println("=== 파일 다운로드 디버깅 ===");
            System.out.println("1. 설정된 uploadPath: " + uploadPath);
            System.out.println("2. 현재 작업 디렉토리: " + System.getProperty("user.dir"));
            System.out.println("3. 절대 경로로 변환된 위치: " + uploadBaseDir);
            System.out.println("4. chat 폴더 경로: " + chatUploadDir);
            System.out.println("5. 최종적으로 찾으려는 파일 경로: " + filePath);
            System.out.println("6. 파일 실제 존재 여부: " + Files.exists(filePath));
            System.out.println("7. 요청된 파일명: " + fileName);
            
            // 실제 uploads/chat 디렉토리 내용 확인
            try {
                if (Files.exists(chatUploadDir)) {
                    System.out.println("7. chat 디렉토리 존재함. 내부 파일 목록:");
                    Files.list(chatUploadDir).forEach(path -> 
                        System.out.println("   - " + path.getFileName()));
                } else {
                    System.out.println("7. chat 디렉토리가 존재하지 않음!");
                }
            } catch (Exception e) {
                System.out.println("7. 디렉토리 목록 조회 실패: " + e.getMessage());
            }
            
            System.out.println("========================================");
            
            // 파일 존재 여부 확인
            if (!Files.exists(filePath)) {
                return org.springframework.http.ResponseEntity.notFound().build();
            }
            
            // 보안: 디렉토리 트래버설 공격 방지
            if (!filePath.normalize().startsWith(chatUploadDir.normalize())) {
                return org.springframework.http.ResponseEntity.badRequest().build();
            }
            
            // 파일을 Resource로 변환
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(filePath.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                return org.springframework.http.ResponseEntity.notFound().build();
            }
            
            // 파일 타입 감지
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            // 원본 파일명 추출 (UUID 제거)
            String originalFileName = fileName;
            
            return org.springframework.http.ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, 
                           "attachment; filename=\"" + originalFileName + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            throw new RuntimeException("파일 다운로드에 실패했습니다: " + e.getMessage(), e);
        }
    }

}