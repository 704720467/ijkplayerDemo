package com.zp.libvideoedit.exceptions;

public class EffectException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5736327268996378800L;

	public EffectException() {
		super();
	}

	public EffectException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public EffectException(String detailMessage) {
		super(detailMessage);
	}

	public EffectException(Throwable throwable) {
		super(throwable);
	}

}
