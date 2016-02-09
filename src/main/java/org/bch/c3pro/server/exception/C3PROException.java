package org.bch.c3pro.server.exception;

/**
 * Exception class raised by c3pro-consumer system
 * @author CHIP-IHL
 */
public class C3PROException extends Exception {
    private Exception innerException;

    /**
     * Constructor method. Creates a new exception with a message
     * @param msg The message
     */
    public C3PROException(String msg) {
        super(msg);
    }

    /**
     * Constructor method. It allows to embed another exception
     * @param msg The message
     * @param e The embedded exception
     */
    public C3PROException(String msg, Exception e) {
        super(msg);
        this.innerException = e;
    }

    /**
     * Returns the embedded exception
     * @return The exception
     */
    public Exception getInnerException() {
        return innerException;
    }

}
