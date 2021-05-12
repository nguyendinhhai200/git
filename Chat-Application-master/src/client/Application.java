package client;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.ConnectException;

public class Application {

    public static void main(String[] args) {
        try {
            Client client = new Client();
            JFrame frame = new JFrame("Messenger Clone 1");
            frame.setContentPane(new AppGUI(client).getRootPane());
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    super.windowClosing(e);
                }

                @Override
                public void windowClosed(WindowEvent e) {
                    try {
                        client.stopConnection();
                    } catch (IOException ioException) {
                    }
                }
            });
        } catch (ConnectException e) {
            System.err.println("Server seems to be offline.");
        } catch (IOException e) {
            System.err.println("Failed to setup your client.");
        }
    }
}
