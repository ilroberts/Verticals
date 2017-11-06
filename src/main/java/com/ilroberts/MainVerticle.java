package com.ilroberts;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.templ.FreeMarkerTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

    private Logger logger = LoggerFactory.getLogger(MainVerticle.class);
    private final FreeMarkerTemplateEngine templateEngine  = FreeMarkerTemplateEngine.create();

    private static final String SQL_CREATE_PAGES_TABLE = "create table if not exists Pages (Id integer identity primary key, Name varchar(255) unique, Content clob)";
    private static final String SQL_GET_PAGE = "select Id, Content from Pages where Name = ?";
    private static final String SQL_CREATE_PAGE = "insert into Pages values (NULL, ?, ?)";
    private static final String SQL_SAVE_PAGE = "update Pages set Content = ? where Id = ?";
    private static final String SQL_ALL_PAGES = "select Name from Pages";
    private static final String SQL_DELETE_PAGE = "delete from Pages where Id = ?";

    private JDBCClient dbClient;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        logger.info("starting main verticle");
        Future<Void> steps = prepareDatabase().compose(v -> startHttpServer());
        steps.setHandler(ar -> {
            if (ar.succeeded()) {
                startFuture.complete();
            } else {
                startFuture.fail(ar.cause());
            }
        });
    }

    private Future<Void> prepareDatabase() {
        logger.info("preparing database");
        Future<Void> future = Future.future();

        dbClient = JDBCClient.createShared(vertx, new JsonObject()
        .put("url", "jdbc:hsqldb:file:db/wiki")
        .put("driver_class", "org.hsqldb.jdbcDriver")
        .put("max_pool_size", 30));

        dbClient.getConnection(ar -> {
            if (ar.failed()) {
                logger.error("could not open a database connection", ar.cause());
                future.fail(ar.cause());
            } else {
                SQLConnection connection = ar.result();
                connection.execute(SQL_CREATE_PAGES_TABLE, create -> {
                    connection.close();
                    if(create.failed()) {
                        logger.error("database preparation error", create.cause());
                        future.fail(create.cause());
                    } else {
                        future.complete();
                    }
                });
            }
        });
        return future;
    }

    private Future<Void> startHttpServer() {
        logger.info("starting http server");
        Future<Void> future = Future.future();

        HttpServer server = vertx.createHttpServer();

        Router router = Router.router(vertx);
        router.get("/").handler(this::indexHandler);
//        router.get("/wiki/:page").handler(this::pageRenderingHandler);
//        router.post().handler(BodyHandler.create());
//        router.post("/save").handler(this::pageUpdateHandler);
//        router.post("/create").handler(this::pageCreateHandler);
//        router.post("/delete").handler(this::pageDeletionHandler);

        server.requestHandler(router::accept).listen(8080, ar -> {
            if (ar.succeeded()) {
                logger.info("http server running on port 8080");
                future.complete();
            } else {
                logger.info("could not start a http server", ar.cause());
                future.fail(ar.cause());
            }
        });

        return future;
    }

    private void indexHandler(RoutingContext context) {
        dbClient.getConnection(car -> {
            if (car.succeeded()) {
                SQLConnection connection = car.result();
                connection.query(SQL_ALL_PAGES, res -> {
                    connection.close();

                    if (res.succeeded()) {
                        List<String> pages = res.result()
                                .getResults()
                                .stream()
                                .map(json -> json.getString(0))
                                .sorted()
                                .collect(Collectors.toList());

                        context.put("title", "Wiki home");
                        context.put("pages", pages);
                        templateEngine.render(context, "templates", "/index.ftl", ar -> {
                            if (ar.succeeded()) {
                                context.response().putHeader("Content-Type", "text/html");
                                context.response().end(ar.result());
                            } else {
                                context.fail(ar.cause());
                            }
                        });
                    } else {
                        context.fail(res.cause());
                    }
                });
            } else {
                context.fail(car.cause());
            }
        });
    }
}
