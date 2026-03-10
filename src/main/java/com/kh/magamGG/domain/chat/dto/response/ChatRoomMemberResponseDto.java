package com.kh.magamGG.domain.chat.dto.response;

import com.kh.magamGG.domain.chat.entity.ChatRoomMember;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatRoomMemberResponseDto {
    private Long memberNo;
    private String memberName;
    private String memberProfile;
    private String joinedAt;
    private Long lastReadChatNo;



    // 엔티티를 DTO로 변환하는 정적 메서드
    public static ChatRoomMemberResponseDto from(ChatRoomMember entity) {
        return ChatRoomMemberResponseDto.builder()
                .memberNo(entity.getMember().getMemberNo())
                .memberName(entity.getMember().getMemberName())
                .joinedAt(entity.getChatRoomMemberJoinedAt().toString())
                .lastReadChatNo(entity.getLastReadChatNo())
                .build();
    }
}