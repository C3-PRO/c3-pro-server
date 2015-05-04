package org.bch.c3pro.server.external;

import org.bch.c3pro.server.exception.C3PROException;

/**
 * Created by CH176656 on 5/4/2015.
 */
public interface Queue {
    public void sendMessage(String resource) throws C3PROException;
}
