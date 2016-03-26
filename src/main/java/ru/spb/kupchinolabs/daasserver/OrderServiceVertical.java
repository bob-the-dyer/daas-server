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
                .addInboundPermitted(new PermittedOptions().setAddress(Constants.ORDER_CREATE));
        sockJSHandler.bridge(options);

        router.route("/eventbus/*").handler(sockJSHandler);
        router.route().handler(StaticHandler.create());
        vertx.createHttpServer().requestHandler(router::accept).listen(8080);
        //TODO add security, tls
        vertx.eventBus().consumer(Constants.ORDER_CREATE, this::create);

        vertx.setPeriodic(10000, this::mockCapturePending); //TODO remove
    }

    private void mockCapturePending(Long aLong) {
        log.log(Level.INFO, "in mockCapturePending");
        orders.entrySet().stream().forEach(entry -> {
            final JsonObject order = entry.getValue();
            if ("pending".equals(order.getString("status"))) {
                order.put("status", "captured");
                order.put("timestamp", format.format(new Date()));
                order.put("courier", "New Courier");
                log.log(Level.INFO, "capturing order #" + order.getLong("id"));
                vertx.eventBus().send(Constants.ORDER_REALTIME_SPECIFIC_PREFIX + order.getLong("id"), order);
                vertx.eventBus().send(Constants.ORDER_REALTIME, order);
            }
        });
    }

    private void create(Message<JsonObject> orderMsg) {
        //TODO validation
        final JsonObject order = orderMsg.body();
        order.put("id", ++idSequence);
        order.put("status", "pending");
        order.put("timestamp", format.format(new Date()));
        log.log(Level.INFO, "adding new order " + order.toString());
        orders.put(idSequence, order);
        orderMsg.reply(order);
        vertx.eventBus().publish(Constants.ORDER_REALTIME, order);
    }

}
