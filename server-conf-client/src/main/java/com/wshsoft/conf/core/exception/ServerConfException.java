package com.wshsoft.conf.core.exception;

/**
 * server conf exception
 *
 * @author Carry_xie 2018-02-01 19:04:52
 */
public class ServerConfException extends RuntimeException {

    private static final long serialVersionUID = 42L;

    public ServerConfException(String msg) {
        super(msg);
    }

    public ServerConfException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public ServerConfException(Throwable cause) {
        super(cause);
    }

}
