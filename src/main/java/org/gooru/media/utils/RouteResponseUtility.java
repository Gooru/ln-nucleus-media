package org.gooru.media.utils;

import org.gooru.media.constants.HttpConstants.HttpStatus;
import org.gooru.media.responses.writers.ResponseWriterBuilder;
import org.slf4j.Logger;

import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public final class RouteResponseUtility {

    private RouteResponseUtility() {
        throw new AssertionError();
    }

    public static void responseHandler(final RoutingContext routingContext, final AsyncResult<Object> reply,
        final Logger LOG) {
        if (reply.succeeded()) {
            new ResponseWriterBuilder(routingContext, reply).build().writeResponse();
        } else {
            int statusCode = routingContext.statusCode();
            if (statusCode == HttpStatus.TOO_LARGE.getCode()) {
                routingContext.response().setStatusCode(HttpStatus.TOO_LARGE.getCode())
                    .end(HttpStatus.TOO_LARGE.getMessage());
            } else {
                LOG.error("Not able to send message", reply.cause());
                routingContext.response().setStatusCode(500).end();
            }

        }
    }

    public static void errorResponseHandler(final RoutingContext routingContext, final Logger LOG,
        final String response, final int statusCode) {
        JsonObject error = new JsonObject(response);
        routingContext.response().setStatusCode(statusCode);
        routingContext.response().end(error.toString());
    }
}
