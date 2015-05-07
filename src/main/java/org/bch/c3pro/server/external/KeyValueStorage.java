package org.bch.c3pro.server.external;

import org.bch.c3pro.server.exception.C3PROException;

/**
 * Created by CH176656 on 5/4/2015.
 */
public interface KeyValueStorage {
    public void put(String key, String value) throws C3PROException;
    public String get(String key) throws C3PROException;
    public byte[] getBinary(String key) throws C3PROException;
}
