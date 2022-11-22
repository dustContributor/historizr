package io.historizr.device;

import java.util.logging.Level;
import java.util.logging.Logger;

import io.historizr.device.api.DataTypeApi;
import io.historizr.device.api.DeviceApi;
import io.historizr.device.api.SignalApi;
import io.historizr.shared.OpsMisc;
import io.historizr.shared.OpsMisc.PassthroughCodec;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Launcher;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ErrorHandler;
import io.vertx.jdbcclient.JDBCPool;

public final class Main extends AbstractVerticle {
	private static final Logger LOGGER = OpsMisc.classLogger();

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
			SampleWorker sampleWorker = null;
			Future<String> deployedWorker = Future.succeededFuture();
			if (cfg.noSampling()) {
				LOGGER.info("Sample worker DISABLED");
			} else {
				sampleWorker = new SampleWorker();
				deployedWorker = vertx.deployVerticle(sampleWorker, new DeploymentOptions()
						.setWorker(true)
						.setConfig(cfg.toJson()))
						.onFailure(ex -> {
							LOGGER.log(Level.SEVERE, "Failed starting the sample worker", (Throwable) ex);
							vertx.close();
						}).onSuccess(e -> {
							LOGGER.info("Deployed sample worker!");
						});
			}
			LOGGER.info("Configuring HTTP api...");
			// Web router.
			var router = Router.router(vertx);
			// Pretty errors.
			router.errorHandler(500, ErrorHandler.create(vertx));
			// Capture data passed as the body of requests.
			router.route().handler(BodyHandler.create(false));
			// Register api endpoints.
			SignalApi.register(vertx, router, jdbc);
			DataTypeApi.register(vertx, router, jdbc);
			DeviceApi.register(vertx, router, jdbc, sampleWorker);
			LOGGER.info("Configured the HTTP api!");
			LOGGER.info("Creating HTTP server...");
			// Create the HTTP server
			if (cfg.noHttpApi()) {
				LOGGER.info("HTTP api DISABLED");
			} else {
				deployedWorker.onSuccess(e -> {
					vertx.createHttpServer()
							// Handle every request using the router
							.requestHandler(router)
							// Start listening
							.listen(cfg.apiPort())
							// Print the port
							.onSuccess(
									server -> LOGGER.info("HTTP server created on port " + server.actualPort() + "!"))
							.onFailure(ex -> {
								LOGGER.log(Level.SEVERE, "Failed starting the HTTP server", (Throwable) ex);
								vertx.close();
							});
				});
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
