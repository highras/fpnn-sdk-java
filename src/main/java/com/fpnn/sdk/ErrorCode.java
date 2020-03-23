package com.fpnn.sdk;

public enum ErrorCode {
    FPNN_EC_OK (0),

    //for proto
    FPNN_EC_PROTO_UNKNOWN_ERROR (10001),
    FPNN_EC_PROTO_NOT_SUPPORTED (10002),

    FPNN_EC_PROTO_INVALID_PACKAGE (10003),
    FPNN_EC_PROTO_JSON_CONVERT (10004),
    FPNN_EC_PROTO_STRING_KEY (10005),
    FPNN_EC_PROTO_MAP_VALUE (10006),
    FPNN_EC_PROTO_METHOD_TYPE (10007),
    FPNN_EC_PROTO_PROTO_TYPE          (10008),
    FPNN_EC_PROTO_KEY_NOT_FOUND       (10009),
    FPNN_EC_PROTO_TYPE_CONVERT        (10010),
    FPNN_EC_PROTO_FILE_SIGN           (10011),

    //for core
    FPNN_EC_CORE_UNKNOWN_ERROR        (20001),
    FPNN_EC_CORE_CONNECTION_CLOSED    (20002),
    FPNN_EC_CORE_TIMEOUT              (20003),
    FPNN_EC_CORE_UNKNOWN_METHOD       (20004),
    FPNN_EC_CORE_ENCODING             (20005),
    FPNN_EC_CORE_DECODING             (20006),
    FPNN_EC_CORE_SEND_ERROR           (20007),
    FPNN_EC_CORE_RECV_ERROR           (20008),
    FPNN_EC_CORE_INVALID_PACKAGE      (20009),
    FPNN_EC_CORE_HTTP_ERROR           (20010),
    FPNN_EC_CORE_WORK_QUEUE_FULL      (20011),
    FPNN_EC_CORE_INVALID_CONNECTION   (20012),
    FPNN_EC_CORE_FORBIDDEN            (20013),
    FPNN_EC_CORE_SERVER_STOPPING      (20014),

    //for other
    FPNN_EC_ZIP_COMPRESS              (30001),
    FPNN_EC_ZIP_DECOMPRESS            (30002);

    private int value;

    ErrorCode (int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
