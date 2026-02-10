package com.kh.magamGG.domain.manager.dto.response;

import com.kh.magamGG.domain.member.entity.ArtistAssignment;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AssignedArtistResponse {

    private Long artistNo;
    private String artistName;
    private String email;
    private String memberPhone;
    private String memberRole;
    private String memberStatus;
    private String memberProfileImage;
    private String currentProjectName;
    private java.time.LocalDateTime attendanceTime;

    public static AssignedArtistResponse from(ArtistAssignment assignment) {
        return new AssignedArtistResponse(
                assignment.getArtist().getMemberNo(),
                assignment.getArtist().getMemberName(),
                assignment.getArtist().getMemberEmail(),
                assignment.getArtist().getMemberPhone(),
                assignment.getArtist().getMemberRole(),
                assignment.getArtist().getMemberStatus(),
                assignment.getArtist().getMemberProfileImage(),
                null, // currentProjectName defaults to null
                null  // attendanceTime defaults to null
        );
    }
}