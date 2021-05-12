package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client {
    private ClientGUI gui;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private final Thread receiver;
    private User user;
    private boolean isLogin = false ;
    public void setIsLogin(boolean b){
        this.isLogin = b;
    }
    public boolean getIsLogin(){
        return this.isLogin;
    }
    public Client() throws IOException {
        startConnection("localhost", 6666);

        receiver = new Thread(() -> {
            Controller controller = new Controller();
            try {
                while (true) {
                    String input = in.readUTF();
                    controller.execute(input);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        receiver.start();
    }

    public void startConnection(String ip, int port) throws IOException {
        socket = new Socket(ip, port);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
    }

    public void stopConnection() throws IOException {
        receiver.interrupt();
        in.close();
        out.close();
        socket.close();
    }

    public void sendMessage(String mes) throws IOException {
        out.writeUTF(mes);
        out.flush();
    }

    public void setGUI(ClientGUI gui) {
        this.gui = gui;
    }

    private class Controller {

        private final Pattern commandRegex = Pattern.compile("^head=(send|return-groups|return-copy|return-register|return-log-in|return-search), body=(.*)", Pattern.DOTALL);
        private final Pattern mesBody = Pattern.compile("^group=(\\d+), message=(.*)");
        private final Pattern groupBody = Pattern.compile("id=(\\d+), name=\"(.*?)(?=\", )");
        private final Pattern searchBody = Pattern.compile("username=(\\w{8,12}), name=(.*?), ");
        private final Pattern loginBody = Pattern.compile("^state=(true|false), (username=(\\w{8,12}), name=(.*), )?info=(.*)");

        public void execute(String command) {
            Matcher matcher = commandRegex.matcher(command);
            if (!matcher.find())
                return;
            String head = matcher.group(1);
            String body = matcher.group(2);
            switch (head) {
                case "send": {
                    Matcher bodyMatcher = mesBody.matcher(body);
                    if (!bodyMatcher.find())
                        return;
                    int group = Integer.parseInt(bodyMatcher.group(1));
                    String mes = bodyMatcher.group(2);
                    gui.receiveMessage(group, mes);
                    break;
                }
                case "return-groups": {
                    List<Group> groups = new ArrayList<>();
                    Matcher bodyMatcher = groupBody.matcher(body);
                    while (bodyMatcher.find()) {
                        System.out.println("return group" + "    " +Integer.parseInt(bodyMatcher.group(1)) + "    "+ bodyMatcher.group(2) );
                        groups.add(new Group(Integer.parseInt(bodyMatcher.group(1)), bodyMatcher.group(2)));
                    }
                    gui.receiveGroups(groups);
                    break;
                }
                case "return-copy": {
                    gui.receiveCopy(body);
                    break;
                }
                case "return-register": {
                    gui.receiveRegisterResponse(body);
                    break;
                }
                case "return-log-in": {
                    Matcher bodyMatcher = loginBody.matcher(body);
                    if (!bodyMatcher.find())
                        return;
                    boolean state = Boolean.parseBoolean(bodyMatcher.group(1));
                    if (state) {
                        user = new User(bodyMatcher.group(3), bodyMatcher.group(4));
                    }
                    gui.receiveLoginResponse(bodyMatcher.group(5));
                    break;
                }
                case "return-search": {
                    List<User> users = new ArrayList<>();
                    Matcher bodyMatcher = searchBody.matcher(body);
                    while (bodyMatcher.find()) {
                        //users.add(new User(bodyMatcher.group(1), bodyMatcher.group(2)));
                    }
                    gui.receiveSearchResponse(users);
                    break;
                }
                default:
                    break;
            }
        }
    }
}