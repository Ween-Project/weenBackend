package com.ween.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ween.dto.request.AddPostCommentRequest;
import com.ween.dto.request.CreatePostRequest;
import com.ween.dto.response.PostCommentResponse;
import com.ween.dto.response.PostResponse;
import com.ween.security.SecurityUtil;
import com.ween.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class PostControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private PostService postService;
    private SecurityUtil securityUtil;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        postService = mock(PostService.class);
        securityUtil = mock(SecurityUtil.class);
        mockMvc = standaloneSetup(new PostController(postService, securityUtil))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        when(securityUtil.getCurrentUserId()).thenReturn("user-1");
    }

    @Test
    void createAndGetPostUseCurrentUser() throws Exception {
        when(postService.createPost(any(), any())).thenReturn(PostResponse.builder().id("post-1").content("Hi").build());
        when(postService.getPost("post-1", "user-1")).thenReturn(PostResponse.builder().id("post-1").content("Hi").build());
        CreatePostRequest request = new CreatePostRequest();
        request.setContent("Hi");

        mockMvc.perform(post("/api/v1/posts")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("post-1"));
        mockMvc.perform(get("/api/v1/posts/post-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Post retrieved successfully"));
    }

    @Test
    void listPostsReturnsPage() throws Exception {
        when(postService.listPosts(any(), any()))
                .thenReturn(new PageImpl<>(List.of(PostResponse.builder().id("post-1").build()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value("post-1"));
    }

    @Test
    void reactionsDelegateToService() throws Exception {
        when(postService.likePost("post-1", "user-1")).thenReturn(PostResponse.builder().id("post-1").likedByMe(true).build());
        when(postService.savePost("post-1", "user-1")).thenReturn(PostResponse.builder().id("post-1").savedByMe(true).build());
        when(postService.repost("post-1", "user-1")).thenReturn(PostResponse.builder().id("post-1").repostedByMe(true).build());

        mockMvc.perform(post("/api/v1/posts/post-1/like")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/posts/post-1/save")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/posts/post-1/repost")).andExpect(status().isOk());

        verify(postService).likePost("post-1", "user-1");
        verify(postService).savePost("post-1", "user-1");
        verify(postService).repost("post-1", "user-1");
    }

    @Test
    void commentsEndpointsWork() throws Exception {
        when(postService.addComment(any(), any(), any()))
                .thenReturn(PostCommentResponse.builder().id("comment-1").content("Nice").build());
        when(postService.listComments(any(), any()))
                .thenReturn(new PageImpl<>(List.of(PostCommentResponse.builder().id("comment-1").build()), PageRequest.of(0, 20), 1));

        AddPostCommentRequest request = new AddPostCommentRequest();
        request.setContent("Nice");

        mockMvc.perform(post("/api/v1/posts/post-1/comments")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("comment-1"));
        mockMvc.perform(get("/api/v1/posts/post-1/comments"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/posts/post-1/comments/comment-1"))
                .andExpect(status().isOk());
    }
}
