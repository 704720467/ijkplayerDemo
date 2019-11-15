package com.zp.libvideoedit.Transcoder;

/**
 * Created by guoxian on 2018/4/27.
 */

public class TranscodeRunTimeException extends RuntimeException {
    public TranscodeRunTimeException(String message) {
        super(message);
    }

    public TranscodeRunTimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
