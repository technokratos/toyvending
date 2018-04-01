package org.dbk.vending.gui;

import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.media.MediaView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import jssc.SerialPort;
import jssc.SerialPortException;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.dbk.vending.devices.DownCommandListener;
import org.dbk.vending.devices.Periphery;
import org.dbk.vending.gui.views.MainPane;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.testfx.api.FxAssert.verifyThat;

public class MainTest extends ApplicationTest {


    MainPane desktopPane;

    SerialPort serialPort;
    DownCommandListener listener;
    private TextField textField;

    @After
    public void after() throws InterruptedException {
        desktopPane.close();
        TimeUnit.MILLISECONDS.sleep(1000);
    }

    @Override
    public void start(Stage stage) throws Exception {
        desktopPane = new MainPane("COM5");

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        double width = screenBounds.getWidth();
        double height = screenBounds.getHeight();
        desktopPane.setId("desktop");
        Scene scene = new Scene(desktopPane, width, height);
        scene.setFill(null);
        stage.setScene(scene);
        stage.show();

        serialPort = new SerialPort("COM6");
        serialPort.openPort();
        listener = new DownCommandListener(serialPort);
        serialPort.addEventListener(listener);

        textField = lookup("#textField").query();
    }


    @Test
    @SneakyThrows
    public void scenario(){
        for (int i = 0; i < 10; i++) {

            buyWithoutChange();
            TimeUnit.MILLISECONDS.sleep(100);
            listener.reset();
            buyWithChange();
            listener.reset();
        }
    }

    @Test
    public void testChangeVideo() throws InterruptedException {
        MediaView mediaView = lookup("#mediaView").query();
        clickOn("#item" + 0);

        String source = mediaView.getMediaPlayer().getMedia().getSource();
        ///anim0.mp4

        clickOn("#item" + 4);
        TimeUnit.MILLISECONDS.sleep(100);
        ///anim1.mp4
        assertNotEquals(mediaView.getMediaPlayer().getMedia().getSource(), source);
        System.out.println();
    }



    @Test
    @Ignore
    public void buyWithoutChange() throws InterruptedException, SerialPortException, ExecutionException, TimeoutException {

        addCashAndVerify(200);
        int position = 1;
        clickOn("#item" + position);

        clickOn("#buy");
        successExctract(position);

        TimeUnit.MILLISECONDS.sleep(100);
        verifyThat(textField, tf -> tf.getText().equals("0 руб."));


    }




    @Test
    @Ignore
    @SneakyThrows
    public void buyWithChange() {

        int position = 2;
        clickOn("#item" + position);

        addCashAndVerify(350);

        clickOn("#buy");
        successExctract(position);

        TimeUnit.MILLISECONDS.sleep(100);
        verifyThat(textField, tf -> tf.getText().equals("50 руб."));

        listener.reset();
        clickOn("#cancel");
        successChange(50);
        TimeUnit.MILLISECONDS.sleep(500);
        verifyThat(textField, tf -> tf.getText().equals("0 руб."));
//
//        TimeUnit.SECONDS.sleep(1);

    }

    @SneakyThrows
    private void successChange(int change) {
        Pair<Periphery.DownCommand, Integer> value = listener.getValue(10);
        assertEquals(value.getKey(), Periphery.DownCommand.TakeCash);
        assertEquals(value.getValue(), Integer.valueOf(change));
        send(Periphery.UpCommand.CashResultSuccess, change);
    }

    private void successExctract(int position) throws ExecutionException, InterruptedException, TimeoutException, SerialPortException {
        Pair<Periphery.DownCommand, Integer> value = listener.getValue(10);
        assertEquals(value.getKey(), Periphery.DownCommand.Extradition );
        assertEquals(value.getValue(), Integer.valueOf(position));
        send(Periphery.UpCommand.ExtraditionResultSuccess, position);
    }

    private void addCashAndVerify(int cash) throws SerialPortException, InterruptedException {
        send(Periphery.UpCommand.ReceiveCash, cash);
        TimeUnit.MILLISECONDS.sleep(300);
        verifyThat(textField, tf -> tf.getText().equals(cash + " руб."));
    }

    private void send(Periphery.UpCommand upCommand, int data) throws SerialPortException {
        serialPort.writeByte(upCommand.getCode());
        Periphery.Type type = upCommand.getType();
        serialPort.writeBytes(type.send(data));
    }
}