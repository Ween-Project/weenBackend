package com.ween.service;

import com.ween.mapper.OrganizationMapper;
import com.ween.mapper.UserMapper;
import com.ween.repository.CertificateRepository;
import com.ween.repository.CoinTransactionRepository;
import com.ween.repository.EventRegistrationRepository;
import com.ween.repository.EventRepository;
import com.ween.repository.OrganizationRepository;
import com.ween.repository.PostRepository;
import com.ween.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock UserRepository userRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock EventRepository eventRepository;
    @Mock EventRegistrationRepository eventRegistrationRepository;
    @Mock CertificateRepository certificateRepository;
    @Mock CoinTransactionRepository coinTransactionRepository;
    @Mock UserMapper userMapper;
    @Mock OrganizationMapper organizationMapper;
    @Mock PostRepository postRepository;
    @InjectMocks AdminService adminService;

    @Test
    void createsWithMockitoDependencies() {
        assertThat(adminService).isNotNull();
    }
}
