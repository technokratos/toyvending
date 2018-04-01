package org.dbk.vending.gui.tools;

import org.dbk.vending.gui.Main;

import java.io.IOException;
import java.util.Scanner;

public class Tools {
    public static String readFile(String pathname) throws IOException {

        StringBuilder fileContents = new StringBuilder();
        Scanner scanner = new Scanner(Main.class.getResourceAsStream(pathname));
        String lineSeparator = System.getProperty("line.separator");

        try {
            while (scanner.hasNextLine()) {
                fileContents.append(scanner.nextLine() + lineSeparator);
            }
            return fileContents.toString();
        } finally {
            scanner.close();
        }
    }
}
