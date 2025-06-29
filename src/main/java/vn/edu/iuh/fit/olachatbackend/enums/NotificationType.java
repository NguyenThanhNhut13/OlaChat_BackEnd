package vn.edu.iuh.fit.olachatbackend.enums;

import lombok.Getter;

@Getter
public enum NotificationType {
    LOGIN_QR("LOGIN_QR"),
    FRIEND_REQUEST("FRIEND_REQUEST"),
    MESSAGE("MESSAGE"),
    MENTION("MENTION"),
    GROUP("GROUP"),
    SYSTEM("SYSTEM"),
    POST_LIKE("POST_LIKE"),
    POST_COMMENT("POST_COMMENT"),
    POST_SHARE("POST_SHARE"),
    POST_COMMENT_REPLY("POST_COMMENT_REPLY"),

//    Call
    CALL_OFFER("CALL_OFFER"),
    CALL_ACCEPTED("CALL_ACCEPTED"),
    CALL_REJECTED("CALL_REJECTED"),
    CALL_CANCELED("CALL_CANCELED"),
    CALL_MISSED("CALL_MISSED"),
    CALL_NO_RESPONSE("CALL_NO_RESPONSE");
    private final String value;

    NotificationType(String value) {
        this.value = value;
    }
}