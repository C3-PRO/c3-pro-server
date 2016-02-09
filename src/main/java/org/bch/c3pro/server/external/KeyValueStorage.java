package org.bch.c3pro.server.external;

import org.bch.c3pro.server.exception.C3PROException;

/**
 * Interface for key-value storage systems
 * @author CHIP-HL
 */
public interface KeyValueStorage {
    public void put(String key, String value) throws C3PROException;
    public String get(String key) throws C3PROException;
    public byte[] getBinary(String key) throws C3PROException;
}
