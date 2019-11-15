package com.zp.libvideoedit.exceptions;

public class EffectRuntimeException extends RuntimeException {

	public EffectRuntimeException() {
		super();
	}

	public EffectRuntimeException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public EffectRuntimeException(String detailMessage) {
		super(detailMessage);
	}

	public EffectRuntimeException(Throwable throwable) {
		super(throwable);
	}

}
