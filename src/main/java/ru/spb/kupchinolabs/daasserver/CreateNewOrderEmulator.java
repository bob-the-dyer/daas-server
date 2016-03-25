package ru.spb.kupchinolabs.daasserver;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

import java.util.logging.Logger;

public class CreateNewOrderEmulator extends AbstractVerticle {

    private final static Logger log = Logger.getLogger(CreateNewOrderEmulator.class.getName());
    private Long globalCounter = 0L;

    @Override
    public void start() throws Exception {
//        vertx.setPeriodic(5000, this::sendRealtime);

    }

    private void sendRealtime(Long aLong) {
        final JsonObject message = new JsonObject();
        message.put("from", "Aptekarsky per, dom " + ++globalCounter);
        message.put("to", "Aptekarsky per, dom " + globalCounter);
        message.put("comment", "I am happy DaaS user");
        vertx.eventBus().send(Constants.ORDER_CREATE, message);

    }

}
