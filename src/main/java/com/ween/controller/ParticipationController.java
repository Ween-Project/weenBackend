package com.ween.controller;

import com.ween.security.SecurityUtil;
import com.ween.service.ParticipationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/participations")
@RequiredArgsConstructor
@Tag(name = "Participations")
public class ParticipationController {

    private final ParticipationService participationService;
    private final SecurityUtil securityUtil;


    @PostMapping("/join/{eventId}")
    public ResponseEntity<String> joinEvent(@PathVariable String eventId) {
        String currentUserId = securityUtil.getCurrentUserId();
        participationService.joinEvent(currentUserId, eventId);
        return ResponseEntity.ok("Successfully joined the event.");
    }


    @PostMapping("/complete/{eventId}")
    public ResponseEntity<String> completeParticipation(@PathVariable String eventId) {
        String currentUserId = securityUtil.getCurrentUserId();

        participationService.completeParticipation(currentUserId, eventId);

        return ResponseEntity.ok("Participation completed and certificate generated.");
    }
}
