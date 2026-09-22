package com.travelmanagementsystem.travel.api;

import com.travelmanagementsystem.shared.security.JwtPrincipal;
import com.travelmanagementsystem.shared.security.Roles;
import com.travelmanagementsystem.travel.application.TravelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('" + Roles.TRAVEL_MANAGER + "', '" + Roles.ADMIN + "')")
    @Operation(
        summary = "Update an existing travel offering",
        description = "Partially updates a travel offering. The owning manager (or an admin) may edit. Only non-null fields are updated. Once PUBLISHED, only description, activities, and transport may change while price, capacity, dates, and title are locked. Requires header X-API-Version: 1.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Travel updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "403", description = "Not the owner or admin", content = @Content),
            @ApiResponse(responseCode = "404", description = "Travel not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Field locked after publication", content = @Content)
        }
    )
    public ResponseEntity<TravelResponse> update(
            @Parameter(description = "Travel ID") @PathVariable Long id,
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody UpdateTravelRequest request) {
        String callerRole = resolveCallerRole();
        TravelResponse response = travelService.updateTravel(id, request, principal.getUserId(), callerRole);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('" + Roles.TRAVEL_MANAGER + "', '" + Roles.ADMIN + "')")
    @Operation(
        summary = "Publish a travel offering",
        description = "Transitions a DRAFT travel to PUBLISHED, making it visible to travelers. The travel must have all required fields populated (title, description, destination, dates, duration, price, capacity, at least one activity and one transport). Only DRAFT travels can be published. Requires header X-API-Version: 1.",
        responses = {
            @ApiResponse(responseCode = "200", description="Travel published successfully"),
            @ApiResponse(responseCode = "403", description = "Not the owner or admin", content = @Content),
            @ApiResponse(responseCode = "404", description = "Travel not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Invalid status transition (travel is not DRAFT)", content = @Content)
        }
    )
    public ResponseEntity<TravelResponse> publish(
            @Parameter(description = "Travel ID") @PathVariable Long id,
            @AuthenticationPrincipal JwtPrincipal principal) {
        String callerRole = resolveCallerRole();
        TravelResponse response = travelService.publish(id, principal.getUserId(), callerRole);
        return ResponseEntity.ok(response);
    }

    private String resolveCallerRole() {
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        
        for (var authority : authorities) {
            String role = authority.getAuthority();
            
            if (role.equals("ROLE_" + Roles.ADMIN)) {
                return Roles.ADMIN;
            }
        }
        return Roles.TRAVEL_MANAGER;
    }
}
