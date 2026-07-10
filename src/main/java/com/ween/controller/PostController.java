package com.ween.controller;

import com.ween.dto.request.AddPostCommentRequest;
import com.ween.dto.request.CreatePostRequest;
import com.ween.dto.request.UpdatePostRequest;
import com.ween.dto.response.ApiResponse;
import com.ween.dto.response.PostCommentResponse;
import com.ween.dto.response.PostResponse;
import com.ween.security.SecurityUtil;
import com.ween.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Posts", description = "Post feed, likes, comments, saves and reposts")
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer")
public class PostController {

    private final PostService postService;
    private final SecurityUtil securityUtil;

    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create post")
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @RequestParam("content") String content,
            @RequestParam(value = "files", required = false) java.util.List<org.springframework.web.multipart.MultipartFile> files) {
        PostResponse response = postService.createPost(securityUtil.getCurrentUserId(), content, files);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Post created successfully"));
    }

    @GetMapping
    @Operation(summary = "List feed posts")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> listPosts(@PageableDefault(size = 20) Pageable pageable) {
        Page<PostResponse> response = postService.listPosts(securityUtil.getCurrentUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, "Posts retrieved successfully"));
    }

    @GetMapping("/{postId}")
    @Operation(summary = "Get post detail")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(@PathVariable String postId) {
        PostResponse response = postService.getPost(postId, securityUtil.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(response, "Post retrieved successfully"));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "List user's posts")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> listUserPosts(
            @PathVariable String userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PostResponse> response = postService.listUserPosts(userId, securityUtil.getCurrentUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, "User posts retrieved successfully"));
    }

    @GetMapping("/organization/{organizationId}")
    @Operation(summary = "List organization's posts")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> listOrganizationPosts(
            @PathVariable String organizationId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PostResponse> response = postService.listOrganizationPosts(
                organizationId, securityUtil.getCurrentUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, "Organization posts retrieved successfully"));
    }

    @GetMapping("/user/{userId}/reposts")
    @Operation(summary = "List user's reposts")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> listUserReposts(
            @PathVariable String userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PostResponse> response = postService.listUserReposts(userId, securityUtil.getCurrentUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, "User reposts retrieved successfully"));
    }

    @GetMapping("/saved")
    @Operation(summary = "List my saved posts")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> listSavedPosts(@PageableDefault(size = 20) Pageable pageable) {
        Page<PostResponse> response = postService.listSavedPosts(securityUtil.getCurrentUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, "Saved posts retrieved successfully"));
    }

    @GetMapping("/liked")
    @Operation(summary = "List my liked posts")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> listLikedPosts(@PageableDefault(size = 20) Pageable pageable) {
        Page<PostResponse> response = postService.listLikedPosts(securityUtil.getCurrentUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, "Liked posts retrieved successfully"));
    }

    @PutMapping(value = "/{postId}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update post")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable String postId,
            @org.springdoc.core.annotations.ParameterObject @Valid @ModelAttribute UpdatePostRequest request,
            @RequestParam(value = "files", required = false) java.util.List<org.springframework.web.multipart.MultipartFile> files) {
        PostResponse response = postService.updatePost(postId, securityUtil.getCurrentUserId(), request, files);
        return ResponseEntity.ok(ApiResponse.ok(response, "Post updated successfully"));
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "Delete post")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable String postId) {
        postService.deletePost(postId, securityUtil.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Post deleted successfully"));
    }

    @PostMapping("/{postId}/like")
    @Operation(summary = "Like post")
    public ResponseEntity<ApiResponse<PostResponse>> likePost(@PathVariable String postId) {
        PostResponse response = postService.likePost(postId, securityUtil.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(response, "Post liked successfully"));
    }

    @DeleteMapping("/{postId}/like")
    @Operation(summary = "Unlike post")
    public ResponseEntity<ApiResponse<PostResponse>> unlikePost(@PathVariable String postId) {
        PostResponse response = postService.unlikePost(postId, securityUtil.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(response, "Post unliked successfully"));
    }

    @PostMapping("/{postId}/save")
    @Operation(summary = "Save post")
    public ResponseEntity<ApiResponse<PostResponse>> savePost(@PathVariable String postId) {
        PostResponse response = postService.savePost(postId, securityUtil.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(response, "Post saved successfully"));
    }

    @DeleteMapping("/{postId}/save")
    @Operation(summary = "Unsave post")
    public ResponseEntity<ApiResponse<PostResponse>> unsavePost(@PathVariable String postId) {
        PostResponse response = postService.unsavePost(postId, securityUtil.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(response, "Post unsaved successfully"));
    }

    @PostMapping("/{postId}/repost")
    @Operation(summary = "Repost")
    public ResponseEntity<ApiResponse<PostResponse>> repost(@PathVariable String postId) {
        PostResponse response = postService.repost(postId, securityUtil.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(response, "Post reposted successfully"));
    }

    @DeleteMapping("/{postId}/repost")
    @Operation(summary = "Remove repost")
    public ResponseEntity<ApiResponse<PostResponse>> unrepost(@PathVariable String postId) {
        PostResponse response = postService.unrepost(postId, securityUtil.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(response, "Repost removed successfully"));
    }

    @PostMapping("/{postId}/comments")
    @Operation(summary = "Add comment")
    public ResponseEntity<ApiResponse<PostCommentResponse>> addComment(
            @PathVariable String postId,
            @Valid @RequestBody AddPostCommentRequest request) {
        PostCommentResponse response = postService.addComment(postId, securityUtil.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Comment added successfully"));
    }

    @GetMapping("/{postId}/comments")
    @Operation(summary = "List comments")
    public ResponseEntity<ApiResponse<Page<PostCommentResponse>>> listComments(
            @PathVariable String postId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PostCommentResponse> response = postService.listComments(postId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, "Comments retrieved successfully"));
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    @Operation(summary = "Delete comment")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable String postId,
            @PathVariable String commentId) {
        postService.deleteComment(postId, commentId, securityUtil.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Comment deleted successfully"));
    }
}
