package io.historizr.device;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Launcher;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ErrorHandler;

public final class Main extends AbstractVerticle {

//	private Main() {
//		throw new RuntimeException();
//	}

	@Override
	public final void start() throws Exception {
		var cfg = Config.read();
		// Db connection shared across handlers.
		var jdbc = JDBCClient.createShared(vertx,
				new JsonObject().put("url", cfg.db()));
//		PRAGMA foreign_keys = ON;
		vertx.deployVerticle(SampleWorker::new, new DeploymentOptions()
				.setWorker(true)
				.setConfig(cfg.toJson()));
		// Web router.
		var router = Router.router(vertx);
		// Pretty errors.
		router.errorHandler(500, ErrorHandler.create(vertx));
		// Capture data passed as the body of requests.
		router.route().handler(BodyHandler.create(false));
		// Register api endpoints.
		io.historizr.device.api.Signal.register(vertx.eventBus(), router, jdbc);
		io.historizr.device.api.DataType.register(vertx.eventBus(), router, jdbc);
		// Create the HTTP server
		vertx.createHttpServer()
				// Handle every request using the router
				.requestHandler(router)
				// Start listening
				.listen(cfg.apiPort())
				// Print the port
				.onSuccess(server -> System.out.println(
						"HTTP server started on port " + server.actualPort()));
	}

	public static void main(String[] args) {
		Launcher.main(args);
	}
}
