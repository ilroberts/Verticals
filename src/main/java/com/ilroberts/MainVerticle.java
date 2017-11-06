package com.ilroberts;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

    Logger logger = LoggerFactory.getLogger(MainVerticle.class);
    public void start(Future<Void> startFuture) throws Exception {

        logger.info("starting main verticle");
        startFuture.complete();
    }
}
