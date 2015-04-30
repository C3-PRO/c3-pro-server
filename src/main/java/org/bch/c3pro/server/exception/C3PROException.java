package org.bch.c3pro.server.exception;

/**
 * Created by CH176656 on 4/29/2015.
 */
public class C3PROException extends Exception {
    private Exception innerException;
    public C3PROException(String msg) {
        super(msg);
    }

    public C3PROException(String msg, Exception e) {
        super(msg);
        this.innerException = e;
    }

    public Exception getInnerException() {
        return innerException;
    }

}
