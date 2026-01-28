package com.kh.magamGG.domain.manager.controller;

import com.kh.magamGG.domain.manager.dto.response.AssignedArtistResponse;
import com.kh.magamGG.domain.manager.service.ArtistAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/managers")
@RequiredArgsConstructor
public class ManagerController {

    private final ArtistAssignmentService assignmentService;

    @PostMapping("/{managerNo}/artists/{artistNo}")
    public void assignArtist(
            @PathVariable Long managerNo,
            @PathVariable Long artistNo
    ) {
        assignmentService.assignArtist(managerNo, artistNo);
    }

    @GetMapping("/{managerNo}/artists")
    public List<AssignedArtistResponse> getAssignedArtists(
            @PathVariable Long managerNo
    ) {
        return assignmentService.getAssignedArtists(managerNo)
                .stream()
                .map(AssignedArtistResponse::from)
                .toList();
    }
}