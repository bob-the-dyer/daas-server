package ru.spb.kupchinolabs.daasserver;

public interface Constants {
    public static final String ORDER_CREATE = "order.create";
    public static final String ORDER_REALTIME = "order.realtime";
    public static final String ORDER_QUERYALL = "order.queryall";
    public static final String ORDER_ACTION = "order.action";
    public static final String ORDER_REALTIME_SPECIFIC_PREFIX = "order.realtime.specific.";

    public static final String ORDER_STATUS_PENDING = "pending";
    public static final String ORDER_STATUS_CAPTURED = "captured";
    public static final String ORDER_STATUS_ENROUTE = "enroute";
    public static final String ORDER_STATUS_PICKEDUP = "pickedup";
    public static final String ORDER_STATUS_DELIVERING = "delivering";
    public static final String ORDER_STATUS_DELIVERED = "delivered";

    public static final String ORDER_ID = "id";
    public static final String FROM = "from";
    public static final String TO = "to";
    public static final String COMMENT = "comment";
    public static final String STATUS = "status";
    public static final String TIMESTAMP = "timestamp";
    public static final String COURIER = "courier";
}
