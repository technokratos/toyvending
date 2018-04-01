package org.dbk.vending.gui;

import com.sun.javafx.application.ParametersImpl;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.dbk.vending.gui.views.MainPane;

public class Main extends Application {

    private Rectangle2D screenBounds;

    MainPane mainPane;
    private Stage stage;

    @Override
    public void init() throws Exception {
        super.init();
        screenBounds = Screen.getPrimary().getVisualBounds();
    }

    @Override
    public void start(Stage stage) throws Exception {
        setUserAgentStylesheet(STYLESHEET_MODENA);
        this.stage = stage;
        screenBounds = Screen.getPrimary().getVisualBounds();
        double width = screenBounds.getWidth();
        double height = screenBounds.getHeight();

        String port = ParametersImpl.getParameters(this).getNamed().getOrDefault("port", "com5");
        mainPane = new MainPane(port);
//        mainPane.init();
//        mainPane.start();

        Scene scene = new Scene(mainPane, width, height);

        scene.setFill(null);
        stage.setScene(scene);


        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.show();
        stage.setFullScreen(true);

    }


    public static void main(String[] args) {
        launch(args);
    }


}