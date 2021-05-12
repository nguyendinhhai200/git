package server;

import client.Client;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Server {
    private ServerSocket serverSocket;
    private List<User> users;
    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        users = new ArrayList<>();
        while (true)
            new ClientHandler(serverSocket.accept()).start();
    }

    public void stop() throws IOException {
        serverSocket.close();
    }

    private User getUserByUsername(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username))
                return user;
        }
        return null;
    }

    private List<User> getUsersByName(String name) {
        List<User> result = new ArrayList<>();
        for (User user : users)
            if (user.getName().toLowerCase().indexOf(name.toLowerCase()) == 0)
                result.add(user);
        return result;
    }

    public static void main(String[] args) {
        Server server = new Server();
        try {
            server.start(6666);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientHandler extends Thread {
        private final Socket clientSocket;
        private User user;
        private final DataInputStream in;
        private final DataOutputStream out;

        public ClientHandler(Socket socket) throws IOException {
            this.clientSocket = socket;
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());
        }

        public void run() {
            try {
                Controller controller = new Controller();
                String inputLine;
                while (true) {
                    inputLine = in.readUTF();
                    controller.execute(inputLine);
                }
            } catch (IOException e) {
                if (user != null)
                    user.removeSocket(clientSocket);
                System.out.println("Client at " + clientSocket.getInetAddress() + " has just gone offline");
            }
            try {
                in.close();
                clientSocket.close();
            } catch (IOException e) {

            }
        }

        public void setUser(User user) {
            this.user = user;
        }

        private class Controller {
            private final Pattern commandRegex = Pattern.compile("^head=(log-in|register|create-group|send|get-groups|get-copy|search), body=(.*)", Pattern.DOTALL);
            private final Pattern registryBody = Pattern.compile("^username=(\\w{8,12}), password=(\\S{6,12}), name=(.*)");
            private final Pattern loginBody = Pattern.compile("^username=(\\w{8,12}), password=(\\S{6,12})");
            private final Pattern groupBody = Pattern.compile("^name=\"(.+)\", members=(.*)");
            private final Pattern mesBody = Pattern.compile("^group-id=(\\d+), message=(.*)");
            private final Pattern searchBody = Pattern.compile("^by=(username|name), key=(.*)");

            public void execute(String command) throws IOException {
                Matcher matcher = commandRegex.matcher(command);
                if (!matcher.find())
                    return;
                String head = matcher.group(1);
                String body = matcher.group(2);
                System.out.println("command" + " : " +  command + "head:  " + head + "body   :" +body) ;

                switch (head) {
                    case "register": {
                        Matcher bodyMatcher = registryBody.matcher(body);
                        String res = "head=return-register, body=";
                        if (!bodyMatcher.find()) {
                            out.writeUTF(res + "Your username or password is invalid!");
                            clientSocket.close();
                            System.out.println("1fsdfsdff");
                            out.flush();
                            return;
                        }
                        String username = bodyMatcher.group(1);
                        String password = bodyMatcher.group(2);
                        String name = bodyMatcher.group(3);
                        boolean isExisted = false;
                        for (User user : users) {
                            if (user.getUsername().equals(username)) {
                                isExisted = true;
                                break;
                            }
                        }
                        if (!isExisted) {
                            users.add(new User(username, password, name));
                            for(int i = 0 ; i < users.size(); i++){
                                System.out.println(users.get(i).getUsername());
                            }
                            res += "Your account has been successfully created!";
                        } else {
                            res += "Username is existed!";
                        }
                        out.writeUTF(res);
                        out.flush();
                        break;
                    }
                    case "log-in": {
                        Matcher bodyMatcher = loginBody.matcher(body);
                        String res = "head=return-log-in, body=state=";
                        if (!bodyMatcher.find()) {
                            out.writeUTF(res + "false, info=Your username or password is invalid!");
                            out.flush();
                            return;
                        }
                        String username = bodyMatcher.group(1);
                        String password = bodyMatcher.group(2);
                        for (User user : users) {
                            if (user.getUsername().equals(username) &&
                                    user.checkPassword(password)) {
                                setUser(user);
                                user.addSocket(clientSocket);
                                out.writeUTF(res + "true, username=" + username + ", name=" + user.getName() + ", info=Logged in!");
                                out.flush();
                                return;
                            }
                        }
                        out.writeUTF(res + "false, info=Your account does not exist!");
                        break;
                    }
                    case "send": {
                        Matcher bodyMatcher = mesBody.matcher(body);
                        if (!bodyMatcher.find())
                            return;
                        int group = Integer.parseInt(bodyMatcher.group(1));
                        String mes = bodyMatcher.group(2);
                        user.sendMessage(user.getGroupByIndex(group), mes);
                        break;
                    }
                    case "create-group": {
                        Matcher bodyMatcher = groupBody.matcher(body);
                        if (!bodyMatcher.find())
                            return;
                        String groupName = bodyMatcher.group(1);
                        String[] usernames = bodyMatcher.group(2).split(" ");
                        List<User> members = new ArrayList<>();
                        for (String username : usernames) {
                            members.add(getUserByUsername(username));
                        }
                        new Group(groupName, members);
                        System.out.println("Group " + groupName + " is created with " + members.size() + " members");
                        break;
                    }
                    case "get-groups": {
                        out.writeUTF("head=return-groups, body=" + user.getGroups());
                        out.flush();
                        break;
                    }
                    case "get-copy": {
                        int groupId = Integer.parseInt(body);
                        out.writeUTF("head=return-copy, body=" + user.getCopy(groupId));
                        out.flush();
                        break;
                    }
                    case "search": {
                        Matcher bodyMatcher = searchBody.matcher(body);
                        if (!bodyMatcher.find())
                            return;
                        String by = bodyMatcher.group(1);
                        String key = bodyMatcher.group(2);
                        StringBuilder result = new StringBuilder("head=return-search, body=");
                        if (by.equals("username")) {
                            User user = getUserByUsername(key);
                            if (user != null)
                                result.append("username=").append(user.getUsername())
                                        .append(", name=").append(user.getName()).append(", ");
                        } else {
                            for (User user : getUsersByName(key))
                                result.append("username=").append(user.getUsername())
                                        .append(", name=").append(user.getName()).append(", ");
                        }
                        out.writeUTF(result.toString());
                        out.flush();
                        break;
                    }
                    default:
                        //Do something else
                        break;
                }
            }
        }
    }
}

class User {
    private final String username;
    private String password;
    private final String name;
    private final List<Group> groups;
    private final List<StringBuilder> copies;
    private final List<Socket> sockets;

    public User(String username, String password, String name) {
        this.username = username;
        this.password = password;
        this.name = name;
        groups = new ArrayList<>();
        copies = new ArrayList<>();
        sockets = new ArrayList<>();
    }
    public boolean GroupIsNull(){
        return groups.size() == 0;
    }
    public String getUsername() {
        return username;
    }

    public boolean checkPassword(String password) {
        return password.equals(this.password);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void addGroup(Group group) {
        groups.add(group);
        copies.add(new StringBuilder());
    }

    public String getGroups() {
        StringBuilder result = new StringBuilder();
        int id = 0;
        for (Group group : groups) {
            result.append("id=").append(id++).append(", name=\"").append(group.getName()).append("\", ");
        }
        return result.toString();
    }

    public void removeCopy(Group group) {
        StringBuilder copy = copies.get(groups.indexOf(group));
        copy.delete(0, copy.length());
    }

    public Group getGroupByIndex(int index) {
        return groups.get(index);
    }

    public String getCopy(int groupId) {
     //   System.out.println(copies.get(groupId));
        return copies.get(groupId).toString();
    }

    public void addSocket(Socket socket) {
        this.sockets.add(socket);
    }

    public void removeSocket(Socket socket) {
        this.sockets.remove(socket);
    }

    public void sendMessage(Group group, String mes) {
        group.sendMessage(this, mes);
    }

    public void receiveMessage(Group group, User sender, String mes) {
        int index = groups.indexOf(group);
        if (sender.equals(this)) {
            mes = "You: " + mes;
        } else {
            mes = sender.getName() + "(" + sender.getUsername() + "): " + mes;
        }
        copies.get(index).append(mes).append("\n");
        for (Socket socket : sockets) {
            try {
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                out.writeUTF("head=send, body=group=" + index + ", message=" + mes);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", sockets=" + sockets +
                '}';
    }
}

class Group {
    private String name;
    private final List<User> members;

    public Group(String name, List<User> members) {
        this.name = name;
        this.members = members;
        for (User member : members) {
            member.addGroup(this);
        }
    }

    public String getName() {
        return name;
    }

    public void rename(String newName) {
        this.name = newName;
    }
    public List<User> getListUser(){
        return members;
    }
    public void addMember(User user) {
        members.add(user);
    }

    public void sendMessage(User sender, String mes) {
        for (User member : members) {
            member.receiveMessage(this, sender, mes);
        }
    }
    public int numberOfGroup(){
        return members.size();
    }
}