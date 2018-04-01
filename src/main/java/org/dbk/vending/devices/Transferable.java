package org.dbk.vending.devices;

public interface Transferable {
    byte[] send(int data);
    int dataReceive(byte[] data);
}
