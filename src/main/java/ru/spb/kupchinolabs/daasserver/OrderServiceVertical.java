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
            vertx.setPeriodic(5000, this::createNewOrder);
            vertx.setPeriodic(10000, this::emulateCaptured); //TODO remove
            vertx.setPeriodic(20000, this::emulateEnroute); //TODO remove
            vertx.setPeriodic(40000, this::emulatePickudUp); //TODO remove
            vertx.setPeriodic(80000, this::emulateDelivering); //TODO remove
            vertx.setPeriodic(160000, this::emulateDelivered); //TODO remove
        }
    }

    private void createNewOrder(Long aLong) {
        final JsonObject message = new JsonObject();
        message.put(Constants.FROM, "Aptekarsky per, dom " + ++globalCounter);
        message.put(Constants.TO, "Aptekarsky per, dom " + globalCounter);
        message.put(Constants.COMMENT, "I am happy DaaS user");
        vertx.eventBus().send(Constants.ORDER_CREATE, message);
    }

    private void action(Message<JsonObject> msg) {
        //TODO add state machine for order transitions
        log.log(Level.INFO, "in action");
        final JsonObject updatedOrder = msg.body();
        final Long id = updatedOrder.getLong(Constants.ORDER_ID);
        log.log(Level.INFO, "current order state: " + orders.get(id));
        log.log(Level.INFO, "new order state    : " + updatedOrder);
        updatedOrder.put(Constants.TIMESTAMP, format.format(new Date()));
        orders.put(id, updatedOrder);
        vertx.eventBus().send(Constants.ORDER_REALTIME_SPECIFIC_PREFIX + updatedOrder.getLong(Constants.ORDER_ID), updatedOrder);
        vertx.eventBus().publish(Constants.ORDER_REALTIME, updatedOrder);
    }

    private void emulateDelivered(Long aLong) {
        log.log(Level.INFO, "in emulateDelivered");
        orders.entrySet().stream().forEach(entry -> {
            final JsonObject order = entry.getValue();
            if (Constants.ORDER_STATUS_DELIVERING.equals(order.getString(Constants.STATUS))) {
                order.put(Constants.STATUS, Constants.ORDER_STATUS_DELIVERED);
                log.log(Level.INFO, "delivered, order #" + order.getLong(Constants.ORDER_ID));
                vertx.eventBus().send(Constants.ORDER_ACTION, order);
            }
        });
    }

    private void emulateDelivering(Long aLong) {
        log.log(Level.INFO, "in emulateDelivering");
        orders.entrySet().stream().forEach(entry -> {
            final JsonObject order = entry.getValue();
            if (Constants.ORDER_STATUS_PICKEDUP.equals(order.getString(Constants.STATUS))) {
                order.put(Constants.STATUS, Constants.ORDER_STATUS_DELIVERING);
                log.log(Level.INFO, "delivering order #" + order.getLong(Constants.ORDER_ID));
                vertx.eventBus().send(Constants.ORDER_ACTION, order);
            }
        });
    }

    private void emulatePickudUp(Long aLong) {
        log.log(Level.INFO, "in emulatePickudUp");
        orders.entrySet().stream().forEach(entry -> {
            final JsonObject order = entry.getValue();
            if (Constants.ORDER_STATUS_ENROUTE.equals(order.getString(Constants.STATUS))) {
                order.put(Constants.STATUS, Constants.ORDER_STATUS_PICKEDUP);
                log.log(Level.INFO, "picking up order #" + order.getLong(Constants.ORDER_ID));
                vertx.eventBus().send(Constants.ORDER_ACTION, order);
            }
        });
    }

    private void emulateEnroute(Long aLong) {
        log.log(Level.INFO, "in emulateEnroute");
        orders.entrySet().stream().forEach(entry -> {
            final JsonObject order = entry.getValue();
            if (Constants.ORDER_STATUS_CAPTURED.equals(order.getString(Constants.STATUS))) {
                order.put(Constants.STATUS, Constants.ORDER_STATUS_ENROUTE);
                log.log(Level.INFO, "enrouting order #" + order.getLong(Constants.ORDER_ID));
                vertx.eventBus().send(Constants.ORDER_ACTION, order);
            }
        });
    }

    private void emulateCaptured(Long aLong) {
        log.log(Level.INFO, "in emulateCaptured");
        orders.entrySet().stream().forEach(entry -> {
            final JsonObject order = entry.getValue();
            if (Constants.ORDER_STATUS_PENDING.equals(order.getString(Constants.STATUS))) {
                order.put(Constants.STATUS, Constants.ORDER_STATUS_CAPTURED);
                order.put(Constants.COURIER, "New Courier");
                log.log(Level.INFO, "capturing order #" + order.getLong(Constants.ORDER_ID));
                vertx.eventBus().send(Constants.ORDER_ACTION, order);
            }
        });
    }

    private void create(Message<JsonObject> orderMsg) {
        //TODO validation
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
