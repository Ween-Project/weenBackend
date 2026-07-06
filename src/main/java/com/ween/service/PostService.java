package com.ween.service;

import com.ween.dto.request.AddPostCommentRequest;
import com.ween.dto.request.CreatePostRequest;
import com.ween.dto.request.UpdatePostRequest;
import com.ween.dto.response.PostAuthorResponse;
import com.ween.dto.response.PostCommentResponse;
import com.ween.dto.response.PostResponse;
import com.ween.entity.*;
import com.ween.exception.ResourceNotFoundException;
import com.ween.repository.*;
import com.ween.mapper.PostMapper;
import com.ween.enums.PostNotificationAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final PostCommentRepository postCommentRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostSaveRepository postSaveRepository;
    private final PostRepostRepository postRepostRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final NotificationService notificationService;
    private final PostMapper postMapper;

    public PostResponse createPost(String currentUserId, CreatePostRequest request) {
        Post.PostBuilder postBuilder = Post.builder()
                .content(request.getContent())
                .mediaUrl(request.getMediaUrl());

        Organization organization = organizationRepository.findById(currentUserId).orElse(null);
        if (organization != null) {
            postBuilder.organizationAuthor(organization);
        } else {
            postBuilder.userAuthor(getUser(currentUserId));
        }

        Post post = postBuilder.build();
        Post saved = postRepository.save(post);
        log.info("Account {} created post {}", currentUserId, saved.getId());
        return getPost(saved.getId(), currentUserId);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> listPosts(String currentUserId, Pageable pageable) {
        return postRepository.findAllPostsWithStats(currentUserId, pageable)
                .map(postMapper::toPostResponse);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> listUserPosts(String userId, String currentUserId, Pageable pageable) {
        User author = getUser(userId);
        return postRepository.findPostsWithStatsByAuthor(author, currentUserId, pageable)
                .map(postMapper::toPostResponse);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> listOrganizationPosts(String organizationId, String currentUserId, Pageable pageable) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));
        return postRepository.findPostsWithStatsByOrganization(organization, currentUserId, pageable)
                .map(postMapper::toPostResponse);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> listSavedPosts(String currentUserId, Pageable pageable) {
        User currentUser = getUser(currentUserId);
        return postSaveRepository.findSavedPostsWithStats(currentUser, currentUserId, pageable)
                .map(postMapper::toPostResponse);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> listLikedPosts(String currentUserId, Pageable pageable) {
        User currentUser = getUser(currentUserId);
        return postLikeRepository.findLikedPostsWithStats(currentUser, currentUserId, pageable)
                .map(postMapper::toPostResponse);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> listUserReposts(String userId, String currentUserId, Pageable pageable) {
        User user = getUser(userId);
        return postRepostRepository.findRepostedPostsWithStats(user, currentUserId, pageable)
                .map(postMapper::toPostResponse);
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(String postId, String currentUserId) {
        return postRepository.findPostWithStatsById(postId, currentUserId)
                .map(postMapper::toPostResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));
    }

    public PostResponse updatePost(String postId, String currentUserId, UpdatePostRequest request) {
        Post post = getInternalPost(postId);
        ensurePostOwner(post, currentUserId);

        post.setContent(request.getContent());
        post.setMediaUrl(request.getMediaUrl());
        postRepository.save(post);
        log.info("User {} updated post {}", currentUserId, postId);
        return getPost(postId, currentUserId);
    }

    public void deletePost(String postId, String currentUserId) {
        Post post = getInternalPost(postId);
        ensurePostOwner(post, currentUserId);

        postLikeRepository.deleteByPost(post);
        postSaveRepository.deleteByPost(post);
        postRepostRepository.deleteByOriginalPost(post);
        postCommentRepository.deleteByPost(post);
        postRepository.delete(post);
        log.info("User {} deleted post {}", currentUserId, postId);
    }

    public PostResponse likePost(String postId, String currentUserId) {
        Post post = getInternalPost(postId);
        User user = getUser(currentUserId);
        if (!postLikeRepository.existsByPostAndUser(post, user)) {
            postLikeRepository.save(PostLike.builder().post(post).user(user).build());
            notifyPostOwner(post, user, PostNotificationAction.LIKE);
        }
        return getPost(postId, currentUserId);
    }

    public PostResponse unlikePost(String postId, String currentUserId) {
        Post post = getInternalPost(postId);
        User user = getUser(currentUserId);
        postLikeRepository.deleteByPostAndUser(post, user);
        return getPost(postId, currentUserId);
    }

    public PostResponse savePost(String postId, String currentUserId) {
        Post post = getInternalPost(postId);
        User user = getUser(currentUserId);
        if (!postSaveRepository.existsByPostAndUser(post, user)) {
            postSaveRepository.save(PostSave.builder().post(post).user(user).build());
        }
        return getPost(postId, currentUserId);
    }

    public PostResponse unsavePost(String postId, String currentUserId) {
        Post post = getInternalPost(postId);
        User user = getUser(currentUserId);
        postSaveRepository.deleteByPostAndUser(post, user);
        return getPost(postId, currentUserId);
    }

    public PostResponse repost(String postId, String currentUserId) {
        Post post = getInternalPost(postId);
        User user = getUser(currentUserId);
        if (!postRepostRepository.existsByOriginalPostAndUser(post, user)) {
            postRepostRepository.save(PostRepost.builder().originalPost(post).user(user).build());
            notifyPostOwner(post, user, PostNotificationAction.REPOST);
        }
        return getPost(postId, currentUserId);
    }

    public PostResponse unrepost(String postId, String currentUserId) {
        Post post = getInternalPost(postId);
        User user = getUser(currentUserId);
        postRepostRepository.deleteByOriginalPostAndUser(post, user);
        return getPost(postId, currentUserId);
    }

    public PostCommentResponse addComment(String postId, String currentUserId, AddPostCommentRequest request) {
        Post post = getInternalPost(postId);
        User author = getUser(currentUserId);
        PostComment comment = PostComment.builder()
                .post(post)
                .author(author)
                .content(request.getContent())
                .build();

        PostComment saved = postCommentRepository.save(comment);
        notifyPostOwnerWithComment(post, author, request.getContent());
        log.info("User {} commented on post {}", currentUserId, postId);
        return postMapper.toCommentResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<PostCommentResponse> listComments(String postId, Pageable pageable) {
        Post post = getInternalPost(postId);
        return postCommentRepository.findByPostOrderByCreatedAtAsc(post, pageable)
                .map(postMapper::toCommentResponse);
    }

    public void deleteComment(String postId, String commentId, String currentUserId) {
        Post post = getInternalPost(postId);
        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        if (!comment.getPost().getId().equals(post.getId())) {
            throw new ResourceNotFoundException("Comment not found for this post");
        }

        boolean commentOwner = comment.getAuthor().getId().equals(currentUserId);
        boolean postOwner = ownsPost(post, currentUserId);
        if (!commentOwner && !postOwner) {
            throw new AccessDeniedException("You can delete only your comment or comments on your post");
        }

        postCommentRepository.delete(comment);
        log.info("User {} deleted comment {} on post {}", currentUserId, commentId, postId);
    }

    private User getUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private Post getInternalPost(String postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));
    }

    private void ensurePostOwner(Post post, String currentUserId) {
        if (!ownsPost(post, currentUserId)) {
            throw new AccessDeniedException("You can modify only your own post");
        }
    }

    private boolean ownsPost(Post post, String accountId) {
        return (post.getUserAuthor() != null && post.getUserAuthor().getId().equals(accountId))
                || (post.getOrganizationAuthor() != null && post.getOrganizationAuthor().getId().equals(accountId));
    }

    private String getPostOwnerAccountId(Post post) {
        if (post.getOrganizationAuthor() != null) {
            return post.getOrganizationAuthor().getId();
        }
        if (post.getUserAuthor() != null) {
            return post.getUserAuthor().getId();
        }
        throw new IllegalStateException("Post must have either a user author or an organization author");
    }

    private void notifyPostOwnerWithComment(Post post, User actor, String comment) {
        String postOwnerId = getPostOwnerAccountId(post);
        if (postOwnerId.equals(actor.getId())) {
            return;
        }

        notificationService.createPostCommentNotification(postOwnerId, actor.getUsername(), comment);
    }


    private void notifyPostOwner(Post post, User actor, PostNotificationAction action) {
        String postOwnerId = getPostOwnerAccountId(post);
        if (postOwnerId.equals(actor.getId())) {
            return;
        }

        if (action == PostNotificationAction.LIKE) {
            notificationService.createPostLikeNotification(postOwnerId, actor.getUsername());
        } else if (action == PostNotificationAction.REPOST) {
            notificationService.createPostRepostNotification(postOwnerId, actor.getUsername());
        }
    }
}