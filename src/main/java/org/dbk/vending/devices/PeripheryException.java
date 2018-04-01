package org.dbk.vending.devices;

import jssc.SerialPortException;

public class PeripheryException extends Exception {
    public PeripheryException(String message) {
        super(message);
    }

    public PeripheryException(String message, SerialPortException e) {
        super(message, e);
    }
}
