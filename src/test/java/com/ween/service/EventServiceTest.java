package com.ween.service;

import com.ween.entity.Event;
import com.ween.exception.ResourceNotFoundException;
import com.ween.mapper.EventMapper;
import com.ween.repository.CertificateRepository;
import com.ween.repository.ChatRoomMemberRepository;
import com.ween.repository.ChatRoomRepository;
import com.ween.repository.EventRegistrationRepository;
import com.ween.repository.EventRepository;
import com.ween.repository.GroupChatMessageRepository;
import com.ween.repository.OrganizationRepository;
import com.ween.repository.ParticipationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock EventRepository eventRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock EventRegistrationRepository registrationRepository;
    @Mock EventMapper eventMapper;
    @Mock OrganizationService organizationService;
    @Mock RegistrationService registrationService;
    @Mock ParticipationRepository participationRepository;
    @Mock CertificateRepository certificateRepository;
    @Mock ChatRoomRepository chatRoomRepository;
    @Mock ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock GroupChatMessageRepository groupChatMessageRepository;
    @InjectMocks EventService eventService;

    @Test
    void getEventByIdReturnsExistingEvent() {
        Event event = Event.builder().title("Cleanup").build();
        event.setId("event-1");
        when(eventRepository.findById("event-1")).thenReturn(Optional.of(event));

        assertThat(eventService.getEventById("event-1")).isSameAs(event);
    }

    @Test
    void getEventByIdFailsWhenMissing() {
        when(eventRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEventById("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
