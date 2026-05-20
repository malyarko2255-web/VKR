package ru.finuniversity.advance.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.finuniversity.advance.common.dto.AdvanceStatus;
import ru.finuniversity.advance.common.dto.AdvanceType;
import ru.finuniversity.advance.common.events.*;
import ru.finuniversity.advance.notification.client.IdentityClient;

import java.math.BigDecimal;
import ru.finuniversity.advance.notification.entity.Notification;
import ru.finuniversity.advance.notification.entity.NotificationTemplate;
import ru.finuniversity.advance.notification.mock.PushClient;
import ru.finuniversity.advance.notification.repository.NotificationRepository;
import ru.finuniversity.advance.notification.repository.NotificationTemplateRepository;
import ru.finuniversity.advance.notification.util.TemplateEngine;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationTemplateRepository templateRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock NotificationDispatcher dispatcher;
    @Mock IdentityClient identityClient;
    @Mock PushClient pushClient;

    @InjectMocks NotificationService notificationService;

    private final TemplateEngine templateEngine = new TemplateEngine();

    private static final UUID DISPATCHER_1 = UUID.randomUUID();
    private static final UUID DISPATCHER_2 = UUID.randomUUID();
    private static final UUID DRIVER_ID    = UUID.randomUUID();
    private static final UUID ADVANCE_ID   = UUID.randomUUID();

    @BeforeEach
    void injectTemplateEngine() throws Exception {
        var field = NotificationService.class.getDeclaredField("templateEngine");
        field.setAccessible(true);
        field.set(notificationService, templateEngine);
    }

    @Test
    void process_advanceCreated_notifiesDispatchers() {
        when(identityClient.getIdsByRole("DISPATCHER"))
                .thenReturn(List.of(DISPATCHER_1, DISPATCHER_2));

        NotificationTemplate tmpl = NotificationTemplate.builder()
                .code("ADVANCE_CREATED")
                .channel("PUSH")
                .titleTemplate("Заявка на аванс создана")
                .bodyTemplate("Заявка {{request_no}} на сумму {{amount}} руб.")
                .active(true)
                .build();
        when(templateRepository.findByCodeAndActiveTrue("ADVANCE_CREATED"))
                .thenReturn(List.of(tmpl));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AdvanceCreatedEvent event = new AdvanceCreatedEvent(
                UUID.randomUUID().toString(),
                ADVANCE_ID.toString(),
                "ADVANCE_CREATED",
                LocalDateTime.now(),
                DRIVER_ID.toString(),
                "route-1",
                AdvanceType.FUEL,
                BigDecimal.valueOf(15000),
                AdvanceStatus.PENDING
        );

        notificationService.process(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());
        verify(dispatcher, times(2)).send(any(Notification.class));

        List<UUID> recipients = captor.getAllValues().stream().map(Notification::getRecipientId).toList();
        assertThat(recipients).containsExactlyInAnyOrder(DISPATCHER_1, DISPATCHER_2);
    }

    @Test
    void process_advanceRejected_notifiesDriverWithReason() {
        NotificationTemplate tmpl = NotificationTemplate.builder()
                .code("ADVANCE_REJECTED")
                .channel("IN_APP")
                .titleTemplate("Заявка отклонена")
                .bodyTemplate("Заявка {{request_no}} отклонена. Причина: {{rejection_reason}}.")
                .active(true)
                .build();
        when(templateRepository.findByCodeAndActiveTrue("ADVANCE_REJECTED"))
                .thenReturn(List.of(tmpl));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String reason = "Превышен лимит на текущий месяц";
        AdvanceRejectedEvent event = new AdvanceRejectedEvent(
                UUID.randomUUID().toString(),
                ADVANCE_ID.toString(),
                "ADVANCE_REJECTED",
                LocalDateTime.now(),
                DRIVER_ID.toString(),
                UUID.randomUUID().toString(),
                reason
        );

        notificationService.process(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        verify(dispatcher).send(any(Notification.class));

        Notification saved = captor.getValue();
        assertThat(saved.getRecipientId()).isEqualTo(DRIVER_ID);
        assertThat(saved.getBody()).contains(reason);
    }

    @Test
    void process_unknownEventType_doesNothing() {
        // AdvanceReportedEvent has no mapping in NotificationService
        AdvanceEvent unknownEvent = new AdvanceReportedEvent(
                UUID.randomUUID().toString(),
                ADVANCE_ID.toString(),
                "ADVANCE_REPORTED",
                LocalDateTime.now(),
                LocalDateTime.now(),
                3,
                java.math.BigDecimal.valueOf(14500)
        );

        notificationService.process(unknownEvent);

        verifyNoInteractions(templateRepository, notificationRepository, dispatcher);
    }
}
