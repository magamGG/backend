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

    public static AssignedArtistResponse from(ArtistAssignment assignment) {
        return new AssignedArtistResponse(
                assignment.getArtist().getMemberNo(),
                assignment.getArtist().getMemberName(),
                assignment.getArtist().getMemberEmail()
        );
    }
}