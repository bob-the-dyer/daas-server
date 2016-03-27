package ru.spb.kupchinolabs.daasserver;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrderServiceVertical extends AbstractVerticle {

    Long idSequence = 0L;
    private Long globalCounter = 0L;
    Map<Long, JsonObject> orders = new HashMap<>();
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private final static Logger log = Logger.getLogger(OrderServiceVertical.class.getName());

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
        BridgeOptions options = new BridgeOptions()
                .addOutboundPermitted(new PermittedOptions().setAddress(Constants.ORDER_QUERYALL))
                .addOutboundPermitted(new PermittedOptions().setAddress(Constants.ORDER_REALTIME))
                .addOutboundPermitted(new PermittedOptions().setAddressRegex(Constants.ORDER_REALTIME_SPECIFIC_PREFIX + ".+"))
                .addInboundPermitted(new PermittedOptions().setAddress(Constants.ORDER_CREATE))
                .addInboundPermitted(new PermittedOptions().setAddress(Constants.ORDER_ACTION));
        sockJSHandler.bridge(options);

        router.route("/eventbus/*").handler(sockJSHandler);
        router.route().handler(StaticHandler.create());
        vertx.createHttpServer().requestHandler(router::accept).listen(8080);//TODO add security, tls

        vertx.eventBus().consumer(Constants.ORDER_CREATE, this::create);
        vertx.eventBus().consumer(Constants.ORDER_ACTION, this::action);

        if (Boolean.getBoolean("emulation")) {
            vertx.setPeriodic(20000, this::createNewOrder);
            vertx.setPeriodic(60000, this::emulateTransition);
        }
    }

    private void createNewOrder(Long aLong) {
        final JsonObject order = TestOrders.orders.get(new Random().nextInt(TestOrders.orders.size()));
        vertx.eventBus().send(Constants.ORDER_CREATE, order);
    }

    private void action(Message<JsonObject> msg) {
        log.log(Level.INFO, "in action");
        final JsonObject updatedOrder = msg.body();
        final Long id = updatedOrder.getLong(Constants.ORDER_ID);
        final JsonObject currentOrder = orders.get(id);
        log.log(Level.INFO, "current order state: " + currentOrder);
        log.log(Level.INFO, "new order state    : " + updatedOrder);
        final String currentStatus = currentOrder.getString(Constants.STATUS);
        final String updatedStatus = updatedOrder.getString(Constants.STATUS);
        if (!updatedStatus.equals(Constants.stateMashine.get(currentStatus))) {
            final String error = "transition from " + currentStatus + " to " + updatedStatus + " is unsupported, aborting";
            log.log(Level.WARNING, error);
            msg.fail(-1, error);
            return;
        }
        updatedOrder.put(Constants.TIMESTAMP, format.format(new Date()));
        orders.put(id, updatedOrder);
        vertx.eventBus().send(Constants.ORDER_REALTIME_SPECIFIC_PREFIX + updatedOrder.getLong(Constants.ORDER_ID), updatedOrder);
        vertx.eventBus().publish(Constants.ORDER_REALTIME, updatedOrder);
        msg.reply(updatedOrder);
    }

    private void emulateTransition(Long aLong) {
        log.log(Level.INFO, "in emulateTransition");
        orders.entrySet().stream().forEach(entry -> {
            final JsonObject existedOrder = entry.getValue();
            final JsonObject orderCopy = existedOrder.copy();
            final String status = orderCopy.getString(Constants.STATUS);
            if (Constants.ORDER_STATUS_DELIVERED.equals(status)) {
                log.log(Level.INFO, "delivered orderCopy #" + orderCopy.getLong(Constants.ORDER_ID) + " was removed");
                orders.remove(orderCopy.getLong(Constants.ORDER_ID));
            } else {
                final Boolean human = orderCopy.getBoolean(Constants.HUMAN);
                if (human != null && human) {
                    log.log(Level.INFO, "orderCopy #" + orderCopy.getLong(Constants.ORDER_ID) + " is skipped from emulation as being processed by human");
                    return;
                }
                final String nextStatus = Constants.stateMashine.get(status);
                log.log(Level.INFO, "orderCopy #" + orderCopy.getLong(Constants.ORDER_ID) + " is changing status from " + status + " to " + nextStatus);
                orderCopy.put(Constants.STATUS, nextStatus);
                vertx.eventBus().send(Constants.ORDER_ACTION, orderCopy);
            }
        });
    }

    private void create(Message<JsonObject> orderMsg) {
        final JsonObject order = orderMsg.body();
        order.put(Constants.ORDER_ID, ++idSequence);
        order.put(Constants.STATUS, Constants.ORDER_STATUS_PENDING);
        order.put(Constants.TIMESTAMP, format.format(new Date()));
        log.log(Level.INFO, "adding new order " + order.toString());
        orders.put(idSequence, order);
        orderMsg.reply(order);
        vertx.eventBus().publish(Constants.ORDER_REALTIME, order);
    }

}
