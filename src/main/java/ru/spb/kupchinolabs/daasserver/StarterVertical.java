package ru.spb.kupchinolabs.daasserver;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import java.util.logging.Level;
import java.util.logging.Logger;

public class StarterVertical extends AbstractVerticle {

    private final static Logger log = Logger.getLogger(StarterVertical.class.getName());

    // Convenience method so you can run it in your IDE
    public static void main(String[] args) {
        System.setProperty("vertx.cwd", "target/classes");
        System.setProperty("vertx.disableFileCaching", "true");
        Vertx.vertx().deployVerticle(new StarterVertical());
    }

    @Override
    public void start() throws Exception {
        log.info("starting to deploy verticals");
        final AsyncResultHandler completionHandler = new AsyncResultHandler();
        log.info("deploying webservice...");
        vertx.deployVerticle(OrderServiceVertical.class.getName(), completionHandler);
        vertx.deployVerticle(RealtimeEmulatorServiceVertical.class.getName(), completionHandler);

    }

    private class AsyncResultHandler implements Handler<AsyncResult<String>> {
        @Override
        public void handle(AsyncResult<String> event) {
            if (event.succeeded()) {
                log.info("deploy succeeded - " + event.result());
            } else {
                log.log(Level.SEVERE, "deploy failed: " + event.cause().getMessage());
                vertx.close();
            }
        }
    }
}
