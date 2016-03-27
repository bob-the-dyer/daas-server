package ru.spb.kupchinolabs.daasserver;

import java.util.HashMap;
import java.util.Map;

public class Constants {
    public static final String ORDER_CREATE = "order.create";
    public static final String ORDER_REALTIME = "order.realtime";
    public static final String ORDER_QUERY_PENDINGS = "order.query.pendings";
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
    public static final String HUMAN = "human";

    public static Map<String, String> stateMashine = new HashMap<>();
    static {
        stateMashine.put(ORDER_STATUS_PENDING, ORDER_STATUS_CAPTURED);
        stateMashine.put(ORDER_STATUS_CAPTURED, ORDER_STATUS_ENROUTE);
        stateMashine.put(ORDER_STATUS_ENROUTE, ORDER_STATUS_PICKEDUP);
        stateMashine.put(ORDER_STATUS_PICKEDUP, ORDER_STATUS_DELIVERING);
        stateMashine.put(ORDER_STATUS_DELIVERING, ORDER_STATUS_DELIVERED);
    }
}
