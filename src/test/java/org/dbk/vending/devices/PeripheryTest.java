package org.dbk.vending.devices;

import jssc.SerialPort;
import jssc.SerialPortException;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class PeripheryTest {

    private SerialPort serialPort;
    private Periphery periphery;
    private DownCommandListener listener;

    @Before
    public void before() throws SerialPortException {
        serialPort = new SerialPort("COM6");
        serialPort.openPort();
        serialPort.setParams(9600, 8, 1, 0);
        periphery = new Periphery("COM5");

        listener = new DownCommandListener(serialPort);
        serialPort.addEventListener(listener);
    }

    @After
    public void after() throws SerialPortException, InterruptedException {
        serialPort.closePort();
        periphery.close();
    }


    @Test
    public void sendDownData() throws PeripheryException, SerialPortException, InterruptedException, ExecutionException, TimeoutException {
        Periphery.DownCommand command = Periphery.DownCommand.Extradition;
        periphery.sendCommand(command, 10);

        Pair<Periphery.DownCommand, Integer> value = listener.getValue(1);
        assertEquals(value.getKey(), command);
        assertEquals(value.getValue(),  Integer.valueOf(10));



    }

    @Test
    public void sendUpData() throws PeripheryException, SerialPortException, InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicReference<Integer> reference = new AtomicReference<>();
        serialPort.setParams(9600, 8, 1, 0);

        Periphery.UpCommand upCommand = Periphery.UpCommand.ExtraditionResultSuccess;
        int data = 11;
        send(upCommand, data);

        periphery.addConsumer(upCommand, value -> {
            countDownLatch.countDown();
            reference.set(value);
        });


        countDownLatch.await(100, TimeUnit.SECONDS);

        assertEquals(reference.get(),  Integer.valueOf(data));



    }

    private void send(Periphery.UpCommand upCommand, int data) throws SerialPortException {
        serialPort.writeByte(upCommand.getCode());
        serialPort.writeBytes(upCommand.getType().send(data));
    }


}