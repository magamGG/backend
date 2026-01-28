package com.kh.magamGG.domain.manager.dto.response;

import com.kh.magamGG.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UnassignedArtistResponse {

    private Long artistNo;
    private String artistName;
    private String email;

    public static UnassignedArtistResponse from(Member artist) {
        return new UnassignedArtistResponse(
                artist.getMemberNo(),
                artist.getMemberName(),
                artist.getMemberEmail()
        );
    }
}
