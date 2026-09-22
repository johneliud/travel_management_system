package com.travelmanagementsystem.travel.api;

import com.travelmanagementsystem.shared.security.JwtPrincipal;
import com.travelmanagementsystem.shared.security.Roles;
import com.travelmanagementsystem.travel.application.TravelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/travels", headers = "X-API-Version=1")
@Tag(name = "Travel Management", description = "Endpoints for creating and managing travel offerings")
public class TravelController {

    private final TravelService travelService;

    public TravelController(TravelService travelService) {
        this.travelService = travelService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('" + Roles.TRAVEL_MANAGER + "', '" + Roles.ADMIN + "')")
    @Operation(
        summary = "Create a new travel offering",
        description = "Creates a new travel offering in DRAFT status. Restricted to Travel Managers and Admins. The manager ID is taken from the authenticated principal. Requires header X-API-Version: 1.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Travel created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content)
        }
    )
    public ResponseEntity<TravelResponse> create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateTravelRequest request) {
        TravelResponse response = travelService.create(request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
