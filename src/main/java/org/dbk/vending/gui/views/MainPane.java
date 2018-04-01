package org.dbk.vending.gui.views;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.InputEvent;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.text.Font;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.dbk.vending.devices.Periphery;
import org.dbk.vending.devices.PeripheryException;
import org.dbk.vending.gui.Main;
import org.dbk.vending.gui.tools.FactoryTools;
import org.dbk.vending.schema.ToySchema;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.dbk.vending.gui.tools.Tools.readFile;

@Slf4j
public class MainPane extends Pane{

    public static final String POSTFIX = " руб.";
    public static final int TIMEOUT_APPEARE_MEDIA = 200;
    public static final int PERIOD_SHOW_MEDIA = 100;

    private static ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

    private ToySchema toySchema;
    private Rectangle2D screenBounds;

    private ImageView itemImage;


    private final Map<Button, ToySchema.Item> buttonItemMap = new HashMap<>();
    private final Map<ToySchema.Item, Button> itemButtonMap = new HashMap<>();
    private final AtomicReference<ToySchema.Item> selectedItem = new AtomicReference<>();
    private WebEngine webEngine;
    private int imageSize;
    private double mediaHeight;
    private double mediaWidth;
    private Pane mediaPane;
    private Periphery periphery;
    private TextField currencyField;
    private Integer currentCash = null;
    private final String port;

    private AtomicReference<State> stateReference = new AtomicReference<>(State.IDLE);

    public MainPane(String port) throws Exception {
        this.port = port;
        init();
        start();
    }

    private void init() throws Exception {
        log.info("Init main pane");
        screenBounds = Screen.getPrimary().getVisualBounds();
        try {
            toySchema = ToySchema.parse(readFile("/schema.xml"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        periphery = new Periphery(this.port);
    }


    private void start() {
        GridPane gridPane = new GridPane();
        this.getChildren().add(gridPane);
        double width = screenBounds.getWidth();
        double height = screenBounds.getHeight();
        mediaHeight = 9 * screenBounds.getHeight() / 22;
        mediaWidth = screenBounds.getWidth();

        ToySchema.Item firstItem = toySchema.getTable().getRowData().iterator().next().getItem().iterator().next();
        MediaView mediaView = getMediaView(firstItem);
        Pane descriptionPane = new Pane();
        GridPane controlButtonPane = new GridPane();
        Pane buttonsPane = new Pane();
        Pane linksPane = new Pane();

        mediaPane = new Pane();
        mediaPane.getChildren().add(mediaView);

        gridPane.add(mediaPane, 0, 0, 3, 9);
        gridPane.add(descriptionPane, 0, 10, 3, 3);
        gridPane.add(controlButtonPane, 1, 13, 2, 1);
        gridPane.add(buttonsPane, 0, 14, 3, 6);
        gridPane.add(linksPane, 0, 20, 3, 1);


        descriptionPane.getChildren().add(initDescription(firstItem));
        currencyField = new TextField("0 " + POSTFIX);
        currencyField.setId("textField");
        currencyField.setEditable(false);
        int buttonHeight = (int) (2 * screenBounds.getHeight() / 22);
        currencyField.setFont(Font.font(buttonHeight / 2));
        Button buyButton = FactoryTools.getButton("buy", buttonHeight, 200, 10, "Купить", "/ios/shopping_cart_loaded.png");

        buyButton.setOnMouseClicked(event -> buy());
        Button cancelButton = FactoryTools.getButton("cancel", buttonHeight, 200, 10, "Сдача", "/ios/cash_receiving.png");
        cancelButton.setOnMouseClicked(event -> cancel());
        controlButtonPane.add(currencyField, 1, 0);
        controlButtonPane.getColumnConstraints().add(new ColumnConstraints(300));
        currencyField.setAlignment(Pos.BASELINE_RIGHT);
        controlButtonPane.setHgap(10);
        controlButtonPane.add(cancelButton, 2, 0);

        controlButtonPane.add(buyButton, 3, 0);
        buyButton.setAlignment(Pos.CENTER_RIGHT);
        buyButton.getStyleClass().add("buy");
        controlButtonPane.setAlignment(Pos.CENTER_RIGHT);
        controlButtonPane.getStylesheets().add(getClass().getResource("/control-buttons.css").toExternalForm());

        showButtons(buttonsPane, new Rectangle2D(0, 0, screenBounds.getWidth(), (6 * screenBounds.getHeight()) / 22));

        periphery.addConsumer(Periphery.UpCommand.ReceiveCash, newValue -> {
                    synchronized (Main.class) {
                        setCurrrentCash(newValue);
                    }
                }
        );

        periphery.addConsumer(Periphery.UpCommand.ExtraditionResultSuccess, extracted -> {
            synchronized (Main.class) {
                if (selectedItem.get() == null || selectedItem.get().getPosition() != extracted) {
                    //todo remove all runTimeException
                    log.warn("Wrong extracted position " + selectedItem.get() + " real extracted " + extracted);
                } else {
                    if (currentCash < selectedItem.get().getPrice()) {
                        //todo
                        log.info("Wrong state of current cash it leads to negative current cash: " + currentCash + " selected price " + selectedItem.get() + " ");
                        setCurrrentCash(0);
                    }
                    setCurrrentCash(currentCash - selectedItem.get().getPrice());
                }
                stateReference.set(State.IDLE);
            }
        });

        periphery.addConsumer(Periphery.UpCommand.ExtraditionResultFail, extracted -> {
            synchronized (Main.class) {
                if (selectedItem.get() == null || selectedItem.get().getPosition() != extracted) {
                    //todo remove all runTimeException
                    System.out.println("Wrong extracted position " + selectedItem.get() + " real extracted " + extracted);
                } else {
                    Button button = itemButtonMap.get(selectedItem.get());
                    button.setDisable(true);
                }
                stateReference.set(State.IDLE);
            }
        });

        periphery.addConsumer(Periphery.UpCommand.CashResultFail, integer -> {
            System.out.println("Cash was't returned");
            //todo show message
            stateReference.set(State.IDLE);
        });
        periphery.addConsumer(Periphery.UpCommand.CashResultSuccess, integer -> {
            synchronized (Main.class) {
                setCurrrentCash(currentCash - integer);
                stateReference.set(State.IDLE);
            }
        });
    }

    private void cancel() {
        State state = stateReference.get();
        if (state == State.WAIT_ANSWER) {
            //todo log
            System.out.println("state is wait");
            return;
        }
        try {
            periphery.sendCommand(Periphery.DownCommand.TakeCash, currentCash);
        } catch (PeripheryException e) {
            e.printStackTrace();
            System.out.println("Error in take cash " + e.getMessage());
            stateReference.set(State.IDLE);
        }

    }

    private void buy() {
        State state = stateReference.get();
        if (state == State.WAIT_ANSWER) {
            log.warn("Try to buy, but state is wait, selected item is {}", selectedItem.get());
            return;
        }
        ToySchema.Item item = selectedItem.get();
        if (item == null) {
            log.warn("Try buy, but the selected item is null");
            return;
        }
        int price = item.getPrice();
        if (price <= currentCash) {
            try {
                stateReference.set(State.WAIT_ANSWER);
                periphery.sendCommand(Periphery.DownCommand.Extradition, item.getPosition());
            } catch (PeripheryException e) {
                e.printStackTrace();
                stateReference.set(State.IDLE);
                log.error("cann't send extradition selected item", selectedItem.get());
            }
        } else {
            //do nothing
        }
    }

    private void setCurrrentCash(Integer newValue) {
        currentCash = newValue;
        currencyField.setText(String.valueOf(newValue) + POSTFIX);
    }

    private MediaView getMediaView(ToySchema.Item item) {
        String firstAnimation = item.getAnimation().getPath();
        Media media = null;
        try {
            media = new Media(getClass().getResource(firstAnimation).toURI().toString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            log.error("Error load media for item " + firstAnimation, e);
        }
        MediaPlayer mediaPlayer = new MediaPlayer(media);


        mediaPlayer.setOnRepeat(() -> {
        });
        mediaPlayer.setOnEndOfMedia(() -> {
            List<ToySchema.Item> items = new ArrayList<>(buttonItemMap.values());
            int index = items.indexOf(item) + 1;
            ToySchema.Item nextItem = index >= items.size() ? items.get(index - items.size()) : items.get(index);
            changeMedia(nextItem);
        });

        mediaPlayer.setAutoPlay(true);
        MediaView mediaView = new MediaView(mediaPlayer);
        mediaView.setId("mediaView");
        mediaView.setFitHeight(mediaHeight);
        mediaView.setFitWidth(mediaWidth);
        mediaView.setPreserveRatio(false);
        return mediaView;
    }

    private GridPane initDescription(ToySchema.Item firstItem) {
        int descrSize = (int) (3 * screenBounds.getHeight() / 22);

        imageSize = descrSize;

        Image image = getImage(firstItem);

        itemImage = new ImageView(image);

        GridPane descriptionGrid = new GridPane();

        WebView descriptionView = new WebView();
        webEngine = descriptionView.getEngine();
        webEngine.loadContent(firstItem.getDescription().getText());

        descriptionGrid.add(itemImage, 0, 0, 1, 1);

        descriptionGrid.add(descriptionView, 1, 0, 2, 1);
        descriptionView.setMinWidth(screenBounds.getWidth() - descrSize - 10);
        descriptionView.setMaxHeight(descrSize);
        descriptionView.setStyle("-fx-background-color: grey;");

        GridPane.setMargin(itemImage, new Insets(10));
        GridPane.setMargin(descriptionView, new Insets(10));
        return descriptionGrid;
    }

    private Image getImage(ToySchema.Item item) {
        return FactoryTools.getImage(item.getImage().getPath(), imageSize, imageSize, true, true);
    }

    private void showButtons(Pane root, Rectangle2D screenBounds) {
        int margin = 10;

        int rows = toySchema.getTable().getRows();
        int columns = toySchema.getTable().getColumns();

        double rowHeight = (screenBounds.getHeight() - 2 * rows * margin) / rows;
        double columnWidth = (screenBounds.getWidth() - 2 * columns * margin) / columns;
        GridPane gridPane = new GridPane();

        gridPane.getStylesheets().add("-fx-background-color: linear-gradient(#777777 0%, #606060 50%, #505250 51%, #2a2b2a 100%);");
        gridPane.getStylesheets().add(getClass().getResource("/buttons.css").toExternalForm());

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                ToySchema.Item item = toySchema.getTable().getRowData().get(row).getItem().get(column);

                Button button = FactoryTools.getItemButton(item, (int) rowHeight, (int) columnWidth, String.valueOf(item.getPrice()));

                buttonItemMap.put(button, item);
                button.setOnKeyPressed(e -> {
                    System.out.println("onkeypressed " + e);
                });

                button.setOnKeyReleased(e -> {
                    System.out.println("onkeypreleased " + e);
                });
                EventHandler<? super InputEvent> eventHandlerClicked = event -> {
                    System.out.println("handler " + event);
                };
                button.setOnTouchPressed(event -> System.out.println("handler touch pressed " + event));
                button.setOnMouseClicked(event -> {
                    if (stateReference.get() == State.IDLE) {
                        ToySchema.Item newItem = buttonItemMap.get(event.getTarget());
                        if (newItem == null) {
                            selectedItem.set(null);
                        } else {
                            selectedItem.set(newItem);
                            webEngine.loadContent(newItem.getDescription().getText());
                            itemImage.setImage(getImage(newItem));
                            changeMedia(newItem);
                        }
                    }
                });

                button.setOnTouchReleased(event -> System.out.println("handler touch released " + event));

                gridPane.add(button, column, row);
                gridPane.setMargin(button, new Insets(margin));
            }
        }

        itemButtonMap.putAll(buttonItemMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey)));

        //box.getChildren().add(button);
        root.getChildren().add(gridPane);

    }

    private void changeMedia(ToySchema.Item selectedItem) {
        MediaView newMediaView = getMediaView(selectedItem);
        Platform.runLater(() -> {
            ObservableList<Node> children = mediaPane.getChildren();
            Node prevMedia = children.get(0);
            //prevMedia.setVisible(false);
            children.remove(prevMedia);
            newMediaView.setOpacity(0);
            children.add(newMediaView);
        });

        service.execute(() -> {
            sleep(TIMEOUT_APPEARE_MEDIA);
            slowShow(newMediaView, true, PERIOD_SHOW_MEDIA);
        });
    }

    private static void slowShow(MediaView newMediaView, boolean show, int periodShowMedia) {
        for (int i = 0; i < 10; i++) {
            double opacity = (show) ? i / 10.0 : 1 - i / 10.0;
            newMediaView.setOpacity(opacity);
            sleep(periodShowMedia / 10);
        }
    }

    private static void sleep(int timeout) {
        try {
            TimeUnit.MILLISECONDS.sleep(timeout);
        } catch (InterruptedException e) {
            log.error("Error", e);
        }
    }

    public void close() {
        periphery.close();
    }


    enum State {
        IDLE,
        WAIT_ANSWER
    }


}
