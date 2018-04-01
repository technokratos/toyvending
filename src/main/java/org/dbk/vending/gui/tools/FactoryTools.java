package org.dbk.vending.gui.tools;

import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import org.dbk.vending.gui.Main;
import org.dbk.vending.schema.ToySchema;

public class FactoryTools {
    public static Button getItemButton(ToySchema.Item item, int height, int width, String text) {
        return getButton("item" + item.getPosition(), height, width, 50, text, item.getImage().getPath());
    }

    public static Button getButton(String id, int height, int width, int imageBorder, String text, String imageFile) {
        ImageView imageView = getImageView(imageFile, height - imageBorder, width - imageBorder);
        imageView.setClip(null);

        Button button = new Button(text, imageView);
        button.setId(id);
        button.setEffect(new DropShadow(10, Color.BLACK));
        button.setMinHeight(height);
        button.setMinWidth(width);
        button.setMaxWidth(width);
        button.setMaxHeight(height);
        return button;
    }

    public static ImageView getImageView(String imageFile, int height, int width) {
        Image imageOk = getImage(imageFile, width, height, true, true);

        ImageView graphic = new ImageView(imageOk);
        return graphic;
    }

    public static Image getImage(String imageFile, double requestedWidth, double requestedHeight, boolean preserveRatio, boolean smooth) {
        return new Image(Main.class.getResourceAsStream(imageFile), requestedWidth, requestedHeight, preserveRatio, smooth);
    }
}
