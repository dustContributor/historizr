package io.historizr.server;

import java.util.logging.Logger;

import io.historizr.server.OpsMisc.PassthroughCodec;
import io.historizr.server.api.DataTypeApi;
import io.historizr.server.api.DeviceApi;
import io.historizr.server.api.DeviceTypeApi;
import io.historizr.server.api.SignalApi;
import io.historizr.server.tmpl.PugTemplateEngine;
import io.historizr.server.view.DeviceView;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Launcher;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ErrorHandler;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;

public final class Main extends AbstractVerticle {
	private final static Logger LOGGER = OpsMisc.classLogger();

	@Override
	public final void start() throws Exception {
		LOGGER.info("Starting...");
		var cfg = Config.read();
		vertx.eventBus().registerCodec(PassthroughCodec.INSTANCE);
		LOGGER.info("Creating JDBC client...");
		var connectOptions = new PgConnectOptions()
				.setPort(cfg.db().port())
				.setHost(cfg.db().host())
				.setDatabase(cfg.db().database())
				.setUser(cfg.db().user())
				.setPassword(cfg.db().pass());

		// Pool options
		var poolOptions = new PoolOptions()
				.setMaxSize(5);

		// Create the client pool
		var jdbc = PgPool.client(vertx, connectOptions, poolOptions);
		LOGGER.info("Created!");
		LOGGER.info("Creating template engine...");
		var tmpl = PugTemplateEngine.create(vertx);
		LOGGER.info("Created!");
		LOGGER.info("Configuring HTTP api...");
		// Web router.
		var router = Router.router(vertx);
		// Pretty errors.
		router.errorHandler(500, ErrorHandler.create(vertx));
		// Capture data passed as the body of requests.
		router.route().handler(BodyHandler.create(false));
		// Register api endpoints.
		DeviceApi.register(vertx, router, jdbc);
		DeviceTypeApi.register(vertx, router, jdbc);
		SignalApi.register(vertx.eventBus(), router, jdbc);
		DataTypeApi.register(vertx.eventBus(), router, jdbc);
		LOGGER.info("Configured!");
		LOGGER.info("Configuring HTTP views...");
		DeviceView.register(vertx, router, tmpl, jdbc);
		LOGGER.info("Configured!");
		LOGGER.info("Starting HTTP server...");
		// Create the HTTP server
		vertx.createHttpServer()
				// Handle every request using the router
				.requestHandler(router)
				// Start listening
				.listen(cfg.port())
				// Print the port
				.onSuccess(server -> LOGGER.info("HTTP server created on port " + server.actualPort() + "!"));
		LOGGER.info("Started!");
	}

	public static void main(String[] args) {
		Launcher.main(args);
	}
}
