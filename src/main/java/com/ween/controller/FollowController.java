package com.ween.controller;

import com.ween.dto.response.ApiResponse;
import com.ween.dto.response.PublicProfileResponse;
import com.ween.security.SecurityUtil;
import com.ween.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Follow", description = "User follow/unfollow and followers endpoints")
@RestController
@RequestMapping("/api/v1/users/{userId}")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;
    private final SecurityUtil securityUtil;
    
    @Operation(summary = "Follow a user", description = "Starts following the specified user by ID")
    @PostMapping("/follow")
    public ResponseEntity<ApiResponse<Void>> followUser(@PathVariable String userId) {
        String currentUserId = securityUtil.getCurrentUserId();
        followService.followUser(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Successfully followed user"));
    }

    @Operation(summary = "Unfollow a user", description = "Stops following the specified user by ID")
    @DeleteMapping("/follow")
    public ResponseEntity<ApiResponse<Void>> unfollowUser(@PathVariable String userId) {
        String currentUserId = securityUtil.getCurrentUserId();
        followService.unfollowUser(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Successfully unfollowed user"));
    }

    @Operation(summary = "Get followers", description = "Retrieves a paginated list of users who follow the specified user")
    @GetMapping("/followers")
    public ResponseEntity<ApiResponse<Page<PublicProfileResponse>>> getFollowers(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<PublicProfileResponse> followers = followService.getFollowers(userId, page, size);
        return ResponseEntity.ok(ApiResponse.ok(followers, "Followers retrieved successfully"));
    }   

    @Operation(summary = "Get following", description = "Retrieves a paginated list of users that the specified user is following")
    @GetMapping("/following")
    public ResponseEntity<ApiResponse<Page<PublicProfileResponse>>> getFollowing(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<PublicProfileResponse> following = followService.getFollowing(userId, page, size);
        return ResponseEntity.ok(ApiResponse.ok(following, "Following retrieved successfully"));
    }
}
