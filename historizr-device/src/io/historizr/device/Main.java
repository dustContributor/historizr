package io.historizr.device;

import java.util.logging.Level;
import java.util.logging.Logger;

import io.historizr.device.OpsMisc.PassthroughCodec;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Launcher;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ErrorHandler;
import io.vertx.jdbcclient.JDBCPool;

public final class Main extends AbstractVerticle {
	private final static Logger LOGGER = Logger.getLogger(Main.class.getName());

	@Override
	public final void start() throws Exception {
		LOGGER.info("Starting...");
		try {
			var cfg = Config.read();
			vertx.eventBus().registerCodec(PassthroughCodec.INSTANCE);
			LOGGER.info("Creating JDBC client...");
			var jdbc = JDBCPool.pool(vertx, new H2Provider().config("cfg", cfg));
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
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Failed to start", e);
			throw e;
		}
	}

	public static void main(String[] args) {
		Launcher.main(args);
	}
}
