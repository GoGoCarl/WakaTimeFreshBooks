package main.java;

public class WFError extends Error {
	
	private static final long serialVersionUID = 1L;

	public WFError() {
		super();
	}

	public WFError(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public WFError(String message, Throwable cause) {
		super(message, cause);
	}

	public WFError(String message) {
		super(message);
	}

	public WFError(Throwable cause) {
		super(cause);
	}

}
