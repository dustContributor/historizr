package io.historizr.device;

import java.util.logging.Logger;

import io.historizr.device.OpsMisc.PassthroughCodec;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Launcher;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ErrorHandler;

public final class Main extends AbstractVerticle {
	private final static Logger LOGGER = Logger.getLogger(Main.class.getName());

	@Override
	public final void start() throws Exception {
		LOGGER.info("Starting...");
		var cfg = Config.read();
		vertx.eventBus().registerCodec(PassthroughCodec.INSTANCE);
		LOGGER.info("Creating JDBC client...");
		var jdbc = JDBCClient.createShared(vertx,
				new JsonObject()
						// Skip all the connection pooling stuff
						.put("provider_class", SQLiteProvider.class.getName())
						.put("cfg", cfg));
		LOGGER.info("Created!");
		LOGGER.info("Deploying sample worker...");
		if (cfg.noSampling()) {
			LOGGER.info("Sampling worker DISABLED");
		} else {
			vertx.deployVerticle(SampleWorker::new, new DeploymentOptions()
					.setWorker(true)
					.setConfig(cfg.toJson()));
			LOGGER.info("Deployed!");
		}
		LOGGER.info("Configuring HTTP api...");
		// Web router.
		var router = Router.router(vertx);
		// Pretty errors.
		router.errorHandler(500, ErrorHandler.create(vertx));
		// Capture data passed as the body of requests.
		router.route().handler(BodyHandler.create(false));
		// Register api endpoints.
		io.historizr.device.api.Signal.register(vertx.eventBus(), router, jdbc);
		io.historizr.device.api.DataType.register(vertx.eventBus(), router, jdbc);
		LOGGER.info("Configured!");
		LOGGER.info("Creating HTTP server...");
		// Create the HTTP server
		if (cfg.noHttpApi()) {
			LOGGER.info("HTTP api DISABLED");
		} else {
			vertx.createHttpServer()
					// Handle every request using the router
					.requestHandler(router)
					// Start listening
					.listen(cfg.apiPort())
					// Print the port
					.onSuccess(server -> LOGGER.info("HTTP server created on port " + server.actualPort() + "!"));
		}
		LOGGER.info("Started!");
	}

	public static void main(String[] args) {
		Launcher.main(args);
	}
}
