package ru.finuniversity.advance.common.util;

public final class KafkaTopics {

    private KafkaTopics() {}

    public static final String ADVANCES_CREATED          = "advances.created";
    public static final String ADVANCES_APPROVED         = "advances.approved";
    public static final String ADVANCES_REJECTED         = "advances.rejected";
    public static final String ADVANCES_PAYMENT_RESULTS  = "advances.payment_results";
    public static final String PAYMENTS_COMMANDS         = "payments.commands";
    public static final String TELEMATICS_EVENTS         = "telematics.events";
    public static final String SCORE_UPDATED             = "scoring.score_updated";
    public static final String LIMITS_CHANGED            = "reference.limits_changed";
    public static final String NOTIFICATIONS_OUTBOX      = "notifications.outbox";
}
