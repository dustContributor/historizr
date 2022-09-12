package io.historizr.server.tmpl;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Map;

import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.exceptions.PugException;
import de.neuland.pug4j.template.PugTemplate;
import de.neuland.pug4j.template.TemplateLoader;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.ext.web.common.template.CachingTemplateEngine;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.common.template.impl.TemplateHolder;

public final class PugTemplateEngine extends CachingTemplateEngine<PugTemplate> implements TemplateEngine {
	/**
	 * This serves both as configuration object and also as engine for generating
	 * the templates.
	 */
	private final PugConfiguration config = new PugConfiguration();

	public static final String TEMPLATE_EXT = "pug";

	public static final PugTemplateEngine create(Vertx vertx) {
		return new PugTemplateEngine(vertx, TEMPLATE_EXT);
	}

	public static final PugTemplateEngine create(Vertx vertx, String extension) {
		return new PugTemplateEngine(vertx, extension);
	}

	public PugTemplateEngine(Vertx vertx, String extension) {
		super(vertx, extension);
		config.setTemplateLoader(new PugTemplateLoader(vertx));
		config.setCaching(false);
	}

	@SuppressWarnings("unchecked")
	@Override
	public final <T> T unwrap() {
		// The config objectis the engine itself
		return (T) config;
	}

	@Override
	public final void render(Map<String, Object> context, String templateFile, Handler<AsyncResult<Buffer>> handler) {
		try {
			var src = adjustLocation(templateFile);
			var tmpl = getOrCompile(src);
			var rendered = config.renderTemplate(tmpl.template(), context);
			handler.handle(Future.succeededFuture(Buffer.buffer(rendered)));
		} catch (Exception ex) {
			handler.handle(Future.failedFuture(ex));
		}
	}

	private final TemplateHolder<PugTemplate> getOrCompile(String src) throws PugException, IOException {
		var tmpl = getTemplate(src);
		if (tmpl != null) {
			// Cached template found, return
			return tmpl;
		}
		// Otherwise load and render
		synchronized (this) {
			tmpl = new TemplateHolder<>(config.getTemplate(src));
		}
		putTemplate(src, tmpl);
		return tmpl;
	}

	public final PugConfiguration config() {
		return config;
	}

	private final class PugTemplateLoader implements TemplateLoader {
		public static final String DEFAULT_BASE_PATH = "";
		private final FileSystem fileSystem;

		PugTemplateLoader(Vertx vertx) {
			this.fileSystem = vertx.fileSystem();
		}

		@Override
		public final long getLastModified(String name) throws IOException {
			String fixd = adjustLocation(name);
			try {
				if (fileSystem.existsBlocking(fixd)) {
					return fileSystem.propsBlocking(fixd).lastModifiedTime();
				}
			} catch (RuntimeException e) {
				throw new IOException(e);
			}
			throw new IOException("Resource not found: " + fixd + ", " + name);
		}

		@Override
		public final Reader getReader(String name) throws IOException {
			String fixd = adjustLocation(name);
			if (!fileSystem.existsBlocking(fixd)) {
				throw new IOException("Resource not found: " + fixd + ", " + name);
			}
			var templ = fileSystem.readFileBlocking(fixd)
					.toString(Charset.defaultCharset());
			return new StringReader(templ);
		}

		@Override
		public final String getExtension() {
			return TEMPLATE_EXT;
		}

		@Override
		public final String getBase() {
			return DEFAULT_BASE_PATH;
		}
	}
}
