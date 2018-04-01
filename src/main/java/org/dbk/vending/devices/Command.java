package org.dbk.vending.devices;

public interface Command {

    byte getCode();

    Periphery.Type getType();
}
