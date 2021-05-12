package client;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;

public class ClientGUI {
    private JTextField inputTextField;
    private JButton sendButton;
    private JPanel rootPane;
    private JTextArea messageTextArea;
    private JButton addButton;
    private JTextField userAdd;
    public static String AddlistUser = "";
    private  boolean isLogin = false;
    public ClientGUI(Client client, String username, String password, int isRegister, String name) {
        client.setGUI(this) ;
        if(isRegister == 0){
            try {
                client.sendMessage("head=register, body=username=" + username + ", password="+password+", name="+ name);
                client.sendMessage("head=log-in, body=username=" + username+", password=" +password);
                inputTextField.setText("");
            //    client.setIsLogin(true);
            } catch (IOException ioException) {
                    System.err.println("ERROR : Sign up ");
            }

        }
        if (isRegister == 1){
            try {
                client.sendMessage("head=log-in, body=username=" + username+", password=" +password);
                inputTextField.setText("");
             //   client.setIsLogin(true);
            } catch (IOException ioException) {
                    System.err.println("ERROR : Login");
            }
            isRegister = -3;
        }
        isRegister = -1;
        sendButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String input = inputTextField.getText();
                if (input.equals("")) {
                    return;
                }

                    try {
                        client.sendMessage("head=send, body=group-id=0, message=" +input);
                        inputTextField.setText("");
                        //delete later
                        messageTextArea.append("You: " + input + "\n");
                    } catch (IOException ioException) {
                    }
              //  }
            }
        });
        addButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    client.sendMessage("head=create-group, body=name=\"Supa vjp pr0\", members=" + username+" "+ userAdd.getText());
                    AddlistUser ="head=create-group, body=name=\"Supa vjp pr0\", members=" + username+" "+ userAdd.getText();
                    userAdd.setText("");
                } catch (IOException ioException) {
//                    System.err.println("Some errors have occurred. Cannot send your message. Try restarting your app.");
                }
            }
        });
        System.out.println("123");
    }

    public void receiveMessage(int group, String mes) {
        //modify later
        messageTextArea.append("Group " + group + ": " + mes + '\n');
    }

    public void receiveGroups(List<Group> groups) {
        //modify later
        StringBuilder list = new StringBuilder();
        for (Group group : groups) {
            list.append(group.getId()).append("-").append(group.getName()).append(" ");
        }
        messageTextArea.append("Group list: " + list + "\n");
    }

    public void receiveCopy(String copy) {
        messageTextArea.append("Copy:\n" + copy + "\n");
    }

    public void receiveRegisterResponse(String res) {
        //modify later
        messageTextArea.append("Register response: " + res + "\n");
    }

    public void receiveLoginResponse(String res) {
        //modify later
        messageTextArea.append("Login response: " + res + "\n");
    }

    public void receiveSearchResponse(List<User> users) {
        //modify later
        StringBuilder list = new StringBuilder();
        for (User user : users) {
            list.append(user.getUsername()).append("-").append(user.getName()).append(" ");
        }
        messageTextArea.append("User list: " + list + "\n");
    }

    public JPanel getRootPane() {
        return rootPane;
    }
}
