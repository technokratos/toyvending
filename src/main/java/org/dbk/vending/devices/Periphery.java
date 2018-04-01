package org.dbk.vending.devices;

import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Periphery {

    private final SerialPort serialPort;
    private final UnCommandListener listener;

    public Periphery(String comPort) {
        serialPort = new SerialPort(comPort);
        try {
            if (!serialPort.openPort()) {
                throw new IllegalStateException("Can not open" + serialPort);
            }
            serialPort.setParams(9600, 8, 1, 0);
            listener = new UnCommandListener(serialPort);
            serialPort.addEventListener(listener);
        } catch (SerialPortException e) {
            throw new IllegalStateException(e);
        }

    }

    public void sendCommand(Command command, int data) throws PeripheryException {
        try {
            if (!serialPort.isOpened()) {
                if (serialPort.openPort()) {
                    throw new PeripheryException("Cannot open port " + serialPort.getPortName());
                }
            }

            if (serialPort.isOpened()) {
                serialPort.writeByte(command.getCode());
                serialPort.writeBytes(command.getType().send(data));
            } else {
                throw new PeripheryException("Cannot open port " + serialPort.getPortName());
            }
        } catch (SerialPortException e) {
            throw new PeripheryException(e.getMessage(), e);
        }
    }

    public void addConsumer(UpCommand command, Consumer<Integer> consumer) {
        listener.addCommand(command, consumer);
    }

    public void close() {
        try {
            serialPort.closePort();
        } catch (SerialPortException e) {
            throw new IllegalStateException(e);
        }
    }


    public enum Type implements Transferable {
//        Byte {
//            @Override
//            public byte[] send(int data) {
//                return new byte[]{(byte) data};
//            }
//
//            @Override
//            public int dataReceive(byte[] data) {
//                return data[0];
//            }
//        },
        TwoBytes {
            // 0x0000 ff aa
            //[ff][aa]
            @Override

            public byte[] send(int data) {
                return ByteBuffer.allocate(2).putShort((short) data).array();
            }

            @Override
            public int dataReceive(byte[] data) {
                return ByteBuffer.wrap(data).getShort();
            }
        };


    }

    public enum DownCommand implements Command {
        Extradition((byte) 11, Type.TwoBytes),
        TakeCash((byte) 12, Type.TwoBytes);

        private final byte code;
        private final Type type;

        DownCommand(byte code, Type type) {
            this.code = code;
            this.type = type;
        }

        @Override
        public byte getCode() {
            return code;
        }

        @Override
        public Type getType() {
            return type;
        }

        public static Pair<DownCommand, Integer> parse(byte[] buffer) {
            System.out.println("Receive buffer " + HexBin.encode(buffer));
            Optional<DownCommand> first = Stream.of(DownCommand.values()).filter(upCommand -> upCommand.code == buffer[0]).findFirst();
            byte[] subBuffer = new byte[buffer.length - 1];
            System.arraycopy(buffer, 1, subBuffer, 0, buffer.length - 1);
            if (first.isPresent()) {
                DownCommand command = first.get();

                return Pair.of(command, command.getType().dataReceive(subBuffer));
            } else {
                System.out.println("Error parse buffer " + HexBin.encode(buffer));
                return null;
            }

        }
    }

    public enum UpCommand implements Command {
        ReceiveCash((byte) 10, Type.TwoBytes),
        ExtraditionResultSuccess((byte) 11, Type.TwoBytes),
        ExtraditionResultFail((byte) 12, Type.TwoBytes),
        CashResultSuccess((byte) 13, Type.TwoBytes),
        CashResultFail((byte) 14, Type.TwoBytes);


        private final byte code;
        private final Type type;

        UpCommand(byte code, Type type) {
            this.code = code;
            this.type = type;
        }

        public byte getCode() {
            return code;
        }

        public Type getType() {
            return type;
        }

        public static Pair<UpCommand, Integer> parse(byte[] buffer) {
            System.out.println("Receive buffer " + HexBin.encode(buffer));
            Optional<UpCommand> first = Stream.of(UpCommand.values()).filter(upCommand -> upCommand.code == buffer[0]).findFirst();
            byte[] subBuffer = new byte[buffer.length - 1];
            System.arraycopy(buffer, 1, subBuffer, 0, buffer.length - 1);
            if (first.isPresent()) {
                UpCommand command = first.get();

                return Pair.of(command, command.type.dataReceive(subBuffer));
            } else {
                System.out.println("Error parse buffer " + HexBin.encode(buffer));
                return null;
            }

        }
    }

    static class UnCommandListener implements SerialPortEventListener {

        private final SerialPort serialPort;

        private final Map<UpCommand, Consumer<Integer>> commandMap = new HashMap<>();

        UnCommandListener(SerialPort serialPort) {
            this.serialPort = serialPort;
        }

        public void addCommand(UpCommand upCommand, Consumer<Integer> consumer) {
            commandMap.put(upCommand, consumer);
        }

        public void serialEvent(SerialPortEvent event) {
            //Object type SerialPortEvent carries information about which event occurred and a value.
            //For example, if the data came a method event.getEventValue() returns us the number of bytes in the input buffer.
            if (event.isRXCHAR()) {
                if (event.getEventValue() == 3) {
                    try {
                        byte buffer[] = serialPort.readBytes(3);

                        Pair<UpCommand, Integer> upCommandAndValue = UpCommand.parse(buffer);
                        if (upCommandAndValue != null) {
                            Consumer<Integer> consumer = commandMap.get(upCommandAndValue.getKey());
                            if (consumer != null) {
                                consumer.accept(upCommandAndValue.getRight());
                            } else {
                                System.out.println("Not found consumer for " + upCommandAndValue.getKey());
                            }
                        }
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

}
