package org.dbk.vending.devices;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class DownCommandListener implements SerialPortEventListener {

    private final SerialPort serialPort;

    private CountDownLatch countDownLatch;

    AtomicReference<Pair<Periphery.DownCommand, Integer>> reference = new AtomicReference<>();

    ExecutorService service = Executors.newSingleThreadScheduledExecutor();
    Future<Pair<Periphery.DownCommand, Integer>> future;

    public DownCommandListener(SerialPort serialPort) {
        this.serialPort = serialPort;
        reset();
    }

    public void reset(){
        countDownLatch = new CountDownLatch(1);
        future = service.submit(() -> {
            try {
                countDownLatch.await();
                return reference.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        });
    }
    public Pair<Periphery.DownCommand, Integer> getValue(long timeout) throws ExecutionException, InterruptedException, TimeoutException {
        return future.get(timeout, TimeUnit.SECONDS);
    }



    public void serialEvent(SerialPortEvent event) {
        //Object type SerialPortEvent carries information about which event occurred and a value.
        //For example, if the data came a method event.getEventValue() returns us the number of bytes in the input buffer.
        if (event.isRXCHAR()) {
            if (event.getEventValue() == 3) {
                try {
                    byte buffer[] = serialPort.readBytes(3);

                    Pair<Periphery.DownCommand, Integer> parse = Periphery.DownCommand.parse(buffer);
                    reference.set(parse);
                    countDownLatch.countDown();

                } catch (SerialPortException ex) {
                    System.out.println(ex);
                }
            }
        }
        //If the CTS line status has changed, then the method event.getEventValue() returns 1 if the line is ON and 0 if it is OFF.
        else if (event.isCTS()) {
            if (event.getEventValue() == 1) {
                System.out.println("CTS - ON");
            } else {
                System.out.println("CTS - OFF");
            }
        } else if (event.isDSR()) {
            if (event.getEventValue() == 1) {
                System.out.println("DSR - ON");
            } else {
                System.out.println("DSR - OFF");
            }
        }
    }
}
