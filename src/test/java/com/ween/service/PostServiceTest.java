package com.ween.service;

import com.ween.dto.projection.PostWithStatsProjection;
import com.ween.dto.request.AddPostCommentRequest;
import com.ween.dto.request.CreatePostRequest;
import com.ween.dto.request.UpdatePostRequest;
import com.ween.dto.response.PostCommentResponse;
import com.ween.dto.response.PostResponse;
import com.ween.entity.*;
import com.ween.exception.ResourceNotFoundException;
import com.ween.mapper.PostMapper;
import com.ween.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private PostCommentRepository postCommentRepository;
    @Mock private PostLikeRepository postLikeRepository;
    @Mock private PostSaveRepository postSaveRepository;
    @Mock private PostRepostRepository postRepostRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private NotificationService notificationService;
    @Mock private PostMapper postMapper;
    @Mock private CloudinaryService cloudinaryService;
    @Mock private FollowRepository followRepository;

    @InjectMocks
    private PostService postService;

    @Test
    void createPostThrowsExceptionWhenEmptyContentAndNoMedia() {
        assertThatThrownBy(() -> postService.createPost("user-1", "", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Post must contain either text content or media files");
    }

    @Test
    void createPostSavesPostByUserAndUploadsMedia() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(cloudinaryService.uploadPostMedia(file)).thenReturn("http://media-url");

        User user = mock(User.class);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(organizationRepository.findById("user-1")).thenReturn(Optional.empty());

        Post post = mock(Post.class);
        when(post.getId()).thenReturn("post-123");
        when(postRepository.save(any(Post.class))).thenReturn(post);

        PostResponse response = mock(PostResponse.class);
        PostWithStatsProjection projection = mock(PostWithStatsProjection.class);
        when(postRepository.findPostWithStatsById("post-123", "user-1")).thenReturn(Optional.of(projection));
        when(postMapper.toPostResponse(projection)).thenReturn(response);

        PostResponse result = postService.createPost("user-1", "Hello Ween", List.of(file));

        verify(cloudinaryService).uploadPostMedia(file);
        verify(postRepository).save(any(Post.class));
        assertThat(result).isEqualTo(response);
    }

    @Test
    void likePostCreatesPostLikeRecordAndSendsNotification() {
        Post post = mock(Post.class);
        User author = mock(User.class);
        when(author.getId()).thenReturn("owner-id");
        when(post.getUserAuthor()).thenReturn(author);
        when(postRepository.findById("post-123")).thenReturn(Optional.of(post));

        User liker = mock(User.class);
        when(liker.getId()).thenReturn("liker-id");
        when(liker.getUsername()).thenReturn("liker_user");
        when(userRepository.findById("liker-id")).thenReturn(Optional.of(liker));

        when(postLikeRepository.existsByPostAndUser(post, liker)).thenReturn(false);

        PostResponse response = mock(PostResponse.class);
        PostWithStatsProjection projection = mock(PostWithStatsProjection.class);
        when(postRepository.findPostWithStatsById("post-123", "liker-id")).thenReturn(Optional.of(projection));
        when(postMapper.toPostResponse(projection)).thenReturn(response);

        postService.likePost("post-123", "liker-id");

        verify(postLikeRepository).save(any(PostLike.class));
        verify(notificationService).createPostLikeNotification("owner-id", "liker_user");
    }

    @Test
    void savePostCreatesPostSaveRecord() {
        Post post = mock(Post.class);
        when(postRepository.findById("post-123")).thenReturn(Optional.of(post));

        User user = mock(User.class);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(postSaveRepository.existsByPostAndUser(post, user)).thenReturn(false);

        PostResponse response = mock(PostResponse.class);
        PostWithStatsProjection projection = mock(PostWithStatsProjection.class);
        when(postRepository.findPostWithStatsById("post-123", "user-1")).thenReturn(Optional.of(projection));
        when(postMapper.toPostResponse(projection)).thenReturn(response);

        postService.savePost("post-123", "user-1");

        verify(postSaveRepository).save(any(PostSave.class));
    }

    @Test
    void repostCreatesPostRepostRecordAndNotifiesOwner() {
        Post post = mock(Post.class);
        User author = mock(User.class);
        when(author.getId()).thenReturn("owner-id");
        when(post.getUserAuthor()).thenReturn(author);
        when(postRepository.findById("post-123")).thenReturn(Optional.of(post));

        User reposter = mock(User.class);
        when(reposter.getId()).thenReturn("reposter-id");
        when(reposter.getUsername()).thenReturn("reposter_user");
        when(userRepository.findById("reposter-id")).thenReturn(Optional.of(reposter));

        when(postRepostRepository.existsByOriginalPostAndUser(post, reposter)).thenReturn(false);

        PostResponse response = mock(PostResponse.class);
        PostWithStatsProjection projection = mock(PostWithStatsProjection.class);
        when(postRepository.findPostWithStatsById("post-123", "reposter-id")).thenReturn(Optional.of(projection));
        when(postMapper.toPostResponse(projection)).thenReturn(response);

        postService.repost("post-123", "reposter-id");

        verify(postRepostRepository).save(any(PostRepost.class));
        verify(notificationService).createPostRepostNotification("owner-id", "reposter_user");
    }

    @Test
    void addCommentCreatesCommentRecordAndNotifiesOwner() {
        Post post = mock(Post.class);
        User author = mock(User.class);
        when(author.getId()).thenReturn("owner-id");
        when(post.getUserAuthor()).thenReturn(author);
        when(postRepository.findById("post-123")).thenReturn(Optional.of(post));

        User commenter = mock(User.class);
        when(commenter.getId()).thenReturn("commenter-id");
        when(commenter.getUsername()).thenReturn("commenter_user");
        when(userRepository.findById("commenter-id")).thenReturn(Optional.of(commenter));

        PostComment comment = mock(PostComment.class);
        when(postCommentRepository.save(any(PostComment.class))).thenReturn(comment);

        AddPostCommentRequest request = new AddPostCommentRequest();
        request.setContent("Great post!");

        postService.addComment("post-123", "commenter-id", request);

        verify(postCommentRepository).save(any(PostComment.class));
        verify(notificationService).createPostCommentNotification("owner-id", "commenter_user", "Great post!");
    }

    @Test
    void deleteCommentThrowsExceptionWhenUnauthorized() {
        Post post = mock(Post.class);
        when(post.getId()).thenReturn("post-123");
        when(postRepository.findById("post-123")).thenReturn(Optional.of(post));

        User commentAuthor = mock(User.class);
        when(commentAuthor.getId()).thenReturn("commenter-id");

        PostComment comment = mock(PostComment.class);
        when(comment.getPost()).thenReturn(post);
        when(comment.getAuthor()).thenReturn(commentAuthor);
        when(postCommentRepository.findById("comment-123")).thenReturn(Optional.of(comment));

        // Malicious user tries to delete
        assertThatThrownBy(() -> postService.deleteComment("post-123", "comment-123", "malicious-user-id"))
                .isInstanceOf(AccessDeniedException.class);
    }
}
