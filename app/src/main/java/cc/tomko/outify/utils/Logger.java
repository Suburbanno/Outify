package cc.tomko.outify.utils;

import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Cross JNI logger implementation
 */
public class Logger {
	private static final ConcurrentMap<String, Logger> LOGGERS = new ConcurrentHashMap<>();

	public static Logger getLogger(Class<?> clazz) {
		return getLogger(clazz.getName());
	}

	public static Logger getLogger(String name) {
		return LOGGERS.computeIfAbsent(name, x -> new Logger(LoggerFactory.getLogger(x)));
	}

	private final org.slf4j.Logger inner;

	private Logger(org.slf4j.Logger inner) {
		this.inner = inner;
	}

	public void error(String msg) {
		this.inner.error(msg);
	}

	public void error(String format, Object... arguments) {
		this.inner.error(format, arguments);
	}

	public void warn(String msg) {
		this.inner.warn(msg);
	}

	public void info(String msg) {
		this.inner.info(msg);
	}

	public void info(String format, Object... arguments) {
		this.inner.info(format, arguments);
	}

	public void debug(String msg) {
		this.inner.debug(msg);
	}

	public void debug(String format, Object... arguments) {
		this.inner.debug(format, arguments);
	}

	public void trace(String msg) {
		this.inner.trace(msg);
	}
}
