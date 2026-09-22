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
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/travels", headers = "X-API-Version=1")
@Tag(name = "Travel Management", description = "Endpoints for creating, managing, and browsing travel offerings")
public class TravelController {

    private final TravelService travelService;

    public TravelController(TravelService travelService) {
        this.travelService = travelService;
    }

    @GetMapping
    @Operation(
        summary = "Browse published travel offerings",
        description = "Returns a paginated list of PUBLISHED travels with optional filtering and sorting. Only published travels are visible. Requires header X-API-Version: 1.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Paginated list of travels"),
            @ApiResponse(responseCode = "400", description = "Invalid filter parameters", content = @Content)
        }
    )
    public ResponseEntity<BrowseTravelsResponse> browse(
            @Parameter(description = "Filter by destination country (case-insensitive)")
            @RequestParam(required = false) String country,
            @Parameter(description = "Filter by destination city (case-insensitive)")
            @RequestParam(required = false) String city,
            @Parameter(description = "Minimum price filter")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price filter")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Filter by start date (on or after)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Filter by end date (on or before)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Filter by activity name (partial match, case-insensitive)")
            @RequestParam(required = false) String activity,
            @Parameter(description = "Sort by: price, start_date, created_at (default: created_at)")
            @RequestParam(defaultValue = "created_at") String sortBy,
            @Parameter(description = "Sort direction: asc or desc (default: desc)")
            @RequestParam(defaultValue = "desc") String sortDirection,
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {

        BrowseTravelsResponse response = travelService.browse(
                country, city, minPrice, maxPrice, startDate, endDate, activity,
                sortBy, sortDirection, page, size);
        return ResponseEntity.ok(response);
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

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('" + Roles.TRAVEL_MANAGER + "', '" + Roles.ADMIN + "')")
    @Operation(
        summary = "List the authenticated manager's own travels",
        description = "Returns all travels owned by the authenticated manager, across all statuses (DRAFT, PUBLISHED, CANCELLED, COMPLETED). Paginated and sorted by creation date descending. Restricted to Travel Managers and Admins. Requires header X-API-Version: 1.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Paginated list of the manager's travels"),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content)
        }
    )
    public ResponseEntity<BrowseTravelsResponse> listMyTravels(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        BrowseTravelsResponse response = travelService.listMyTravels(principal.getUserId(), page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get full detail of a travel offering",
        description = "Returns full detail including activities and transport. PUBLISHED/COMPLETED travels are visible to all authenticated users. DRAFT/CANCELLED travels are visible only to the owning manager or an admin. Returns 404 for unauthorized access to avoid leaking existence of draft content. Requires header X-API-Version: 1.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Travel detail returned successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
            @ApiResponse(responseCode = "404", description = "Travel not found or not visible", content = @Content)
        }
    )
    public ResponseEntity<TravelResponse> getTravel(
            @Parameter(description = "Travel ID") @PathVariable Long id,
            @AuthenticationPrincipal JwtPrincipal principal) {
        String callerRole = resolveCallerRole();
        TravelResponse response = travelService.getTravel(id, principal.getUserId(), callerRole);
        return ResponseEntity.ok(response);
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

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('" + Roles.TRAVEL_MANAGER + "', '" + Roles.ADMIN + "')")
    @Operation(
        summary = "Cancel a travel offering",
        description = "Transitions a DRAFT or PUBLISHED travel to CANCELLED. COMPLETED travels cannot be cancelled. Already-cancelled travels return an error. Requires header X-API-Version: 1.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Travel cancelled successfully"),
            @ApiResponse(responseCode = "403", description = "Not the owner or admin", content = @Content),
            @ApiResponse(responseCode = "404", description = "Travel not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Invalid status transition (COMPLETED or already CANCELLED)", content = @Content)
        }
    )
    public ResponseEntity<TravelResponse> cancel(
            @Parameter(description = "Travel ID") @PathVariable Long id,
            @AuthenticationPrincipal JwtPrincipal principal) {
        String callerRole = resolveCallerRole();
        TravelResponse response = travelService.cancel(id, principal.getUserId(), callerRole);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('" + Roles.TRAVEL_MANAGER + "', '" + Roles.ADMIN + "')")
    @Operation(
        summary = "Mark a travel offering as completed",
        description = "Transitions a PUBLISHED travel to COMPLETED. Only PUBLISHED travels can be completed. Requires header X-API-Version: 1.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Travel marked as completed"),
            @ApiResponse(responseCode = "403", description = "Not the owner or admin", content = @Content),
            @ApiResponse(responseCode = "404", description = "Travel not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Invalid status transition (travel is not PUBLISHED)", content = @Content)
        }
    )
    public ResponseEntity<TravelResponse> complete(
            @Parameter(description = "Travel ID") @PathVariable Long id,
            @AuthenticationPrincipal JwtPrincipal principal) {
        String callerRole = resolveCallerRole();
        TravelResponse response = travelService.complete(id, principal.getUserId(), callerRole);
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
