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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class OrderServiceVertical extends AbstractVerticle {

    Long idSequence = 0L;
    Map<Long, JsonObject> orders = new HashMap<>();

    private final static Logger log = Logger.getLogger(OrderServiceVertical.class.getName());

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
        BridgeOptions options = new BridgeOptions()
                .addOutboundPermitted(new PermittedOptions().setAddress(Constants.ORDER_QUERYALL))
                .addOutboundPermitted(new PermittedOptions().setAddress(Constants.ORDER_REALTIME))
                .addInboundPermitted(new PermittedOptions().setAddress(Constants.ORDER_CREATE))
                .addInboundPermitted(new PermittedOptions().setAddress(Constants.ORDER_CAPTURE));
        sockJSHandler.bridge(options);

        router.route("/eventbus/*").handler(sockJSHandler);
        router.route().handler(StaticHandler.create());
        vertx.createHttpServer().requestHandler(router::accept).listen(8080);
        //TODO add security, tls
        vertx.eventBus().consumer(Constants.ORDER_CREATE, this::create);
    }

    private void create(Message<JsonObject> orderMsg) {
        //TODO validation
        final JsonObject order = orderMsg.body();
        order.put("id", ++idSequence);
        order.put("status", "pending");
        orders.put(idSequence, order);
        orderMsg.reply("success");
        vertx.eventBus().publish(Constants.ORDER_REALTIME, order);
    }

}
