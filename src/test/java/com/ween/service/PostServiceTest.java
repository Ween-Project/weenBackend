package com.ween.service;

import com.ween.mapper.PostMapper;
import com.ween.repository.OrganizationRepository;
import com.ween.repository.PostCommentRepository;
import com.ween.repository.PostLikeRepository;
import com.ween.repository.PostRepository;
import com.ween.repository.PostRepostRepository;
import com.ween.repository.PostSaveRepository;
import com.ween.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock PostRepository postRepository;
    @Mock PostCommentRepository postCommentRepository;
    @Mock PostLikeRepository postLikeRepository;
    @Mock PostSaveRepository postSaveRepository;
    @Mock PostRepostRepository postRepostRepository;
    @Mock UserRepository userRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock NotificationService notificationService;
    @Mock PostMapper postMapper;
    @InjectMocks PostService postService;

    @Test
    void createsWithMockitoDependencies() {
        assertThat(postService).isNotNull();
    }
}
