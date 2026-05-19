-- advance-notification V1: уведомления и шаблоны

CREATE TABLE notification_templates (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    code           VARCHAR(50) UNIQUE NOT NULL,
    channel        VARCHAR(20) NOT NULL,
    title_template TEXT,
    body_template  TEXT        NOT NULL,
    active         BOOLEAN     DEFAULT TRUE
);

CREATE TABLE notifications (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id  UUID        NOT NULL,
    channel       VARCHAR(20) NOT NULL,
    template_code VARCHAR(50) NOT NULL,
    title         VARCHAR(200),
    body          TEXT        NOT NULL,
    advance_id    UUID,
    status        VARCHAR(20) DEFAULT 'PENDING',
    sent_at       TIMESTAMPTZ,
    error_message TEXT,
    created_at    TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_notifications_recipient  ON notifications(recipient_id);
CREATE INDEX idx_notifications_advance    ON notifications(advance_id);
CREATE INDEX idx_notifications_status     ON notifications(status, created_at);

-- Шаблоны уведомлений
INSERT INTO notification_templates (id, code, channel, title_template, body_template) VALUES
    (gen_random_uuid(), 'ADVANCE_CREATED',  'PUSH',
     'Заявка на аванс создана',
     'Ваша заявка {{request_no}} на сумму {{amount}} руб. принята и ожидает рассмотрения.'),

    (gen_random_uuid(), 'ADVANCE_CREATED',  'IN_APP',
     'Заявка на аванс создана',
     'Заявка {{request_no}} создана. Тип: {{advance_type}}, сумма: {{amount}} руб.'),

    (gen_random_uuid(), 'ADVANCE_APPROVED', 'PUSH',
     'Заявка одобрена',
     'Заявка {{request_no}} на сумму {{amount}} руб. одобрена. Ожидайте перевод.'),

    (gen_random_uuid(), 'ADVANCE_APPROVED', 'IN_APP',
     'Заявка одобрена',
     'Заявка {{request_no}} одобрена. Средства будут переведены в ближайшее время.'),

    (gen_random_uuid(), 'ADVANCE_REJECTED', 'PUSH',
     'Заявка отклонена',
     'Заявка {{request_no}} отклонена. Причина: {{rejection_reason}}.'),

    (gen_random_uuid(), 'ADVANCE_REJECTED', 'IN_APP',
     'Заявка отклонена',
     'Заявка {{request_no}} отклонена. Причина: {{rejection_reason}}.'),

    (gen_random_uuid(), 'ADVANCE_PAID',     'PUSH',
     'Аванс выплачен',
     'По заявке {{request_no}} выполнен перевод {{amount}} руб. на ваш счёт СБП.'),

    (gen_random_uuid(), 'ADVANCE_PAID',     'SMS',
     NULL,
     'Аванс {{amount}} руб. по заявке {{request_no}} зачислен. {{bank_name}}'),

    (gen_random_uuid(), 'ADVANCE_OVERDUE_REPORT', 'PUSH',
     'Не сдан отчёт по авансу',
     'По заявке {{request_no}} истёк срок сдачи отчётных документов. Пожалуйста, предоставьте документы.'),

    (gen_random_uuid(), 'ADVANCE_OVERDUE_REPORT', 'EMAIL',
     'Напоминание: не сдан отчёт по авансу {{request_no}}',
     'Уважаемый {{full_name}}, по заявке {{request_no}} от {{created_at}} истёк срок сдачи отчётных документов. Просим предоставить документы в течение 3 рабочих дней.');
