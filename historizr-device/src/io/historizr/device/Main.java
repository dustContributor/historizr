package io.historizr.device;

import io.historizr.device.db.Db;
import io.vertx.core.AbstractVerticle;
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

	static final Object LOCK = new Object();

	@Override
	public void start() throws Exception {
		var cfg = Config.read();
		// Db connection shared across handlers.
		var jdbc = JDBCClient.createShared(vertx,
				new JsonObject().put("url", cfg.db()));
//		PRAGMA foreign_keys = ON;
		// Web router.
		var router = Router.router(vertx);
		// Pretty errors.
		router.errorHandler(500, ErrorHandler.create(vertx));
		// Capture data passed as the body of requests.
		router.route().handler(BodyHandler.create(false));
		// Register api endpoints.
		io.historizr.device.api.Signal.register(router, jdbc);
		io.historizr.device.api.DataType.register(router, jdbc);

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

	public static void main3(String[] args) {
		System.out.println(Db.Sql.QUERY_SIGNAL);
	}

	public static void main(String[] args) {
		Launcher.main(args);
	}

	public static void main2(String[] args) {
		var cfg = Config.read();
		var signalRepo = new SignalRepo(cfg);
		signalRepo.init();
		try (var sampleRepo = new SampleRepo(cfg)) {
			sampleRepo.init(signalRepo);
			if (cfg.hasDebugTopic()) {
				sampleRepo.debugOutput();
			}
			sampleRepo.subscribe();
			synchronized (LOCK) {
				LOCK.wait();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
