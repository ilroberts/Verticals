package com.ilroberts;

import io.vertx.core.Vertx;

public class Main   {

    public static void main(String[] args) {
        // Create an HTTP server which simply returns "Hello World!" to each request.
        Vertx.vertx().deployVerticle(new MainVerticle());
    }


}
