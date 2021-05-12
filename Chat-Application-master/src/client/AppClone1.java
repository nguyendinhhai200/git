package client;

import javax.swing.*;
import java.io.IOException;

public class AppClone1 {
    public static void main(String[] args) {
        try {
            Client client = new Client();
            JFrame frame = new JFrame("Messenger Clone 2");
            frame.setContentPane(new AppGUI(client).getRootPane());
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
