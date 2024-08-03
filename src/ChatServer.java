import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

final class ChatServer {
    static final int SERVER_TEST_PORT = 12345;

    public static void main(String[] args) {
        ChatServer server = new ChatServer(SERVER_TEST_PORT);
        server.execute();
    }

    private final int port;
    private final Set<UserThread> users;
    private final Map<String, ChatRoom> privateChatRooms;

    ChatServer(int port) {
        this.port = port;
        this.users = Collections.synchronizedSet(new HashSet<>());
        this.privateChatRooms = Collections.synchronizedMap(new HashMap<>());
    }

    void execute() {
        try (ServerSocket server = new ServerSocket(port)) {
            System.err.println("Chat server is listening on port: " + port);

            while (true) {
                Socket client = server.accept();
                System.err.println("Client connected.");

                BufferedReader fromUser = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String username = fromUser.readLine();
                System.out.println(username + " ime ");

                // We dispatch a new thread for each user in the chat
                UserThread user = new UserThread(client, this, username);
                this.users.add(user);
                user.start();
                notifyAllUsers();
            }
        } catch (IOException ex) {
            System.err.println("Server errored: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    void notifyAllUsers() {
        List<String> userNames = getUserNames();
        synchronized (this.users) {
            for (UserThread user : users) {
                user.sendMessage("UPDATE_USERS " + String.join(",", userNames));
            }
        }
    }

    void broadcast(UserThread sender, String message) {
        synchronized (this.users) {
            this.users.stream()
                    .filter(u -> u != sender)
                    .forEach(u -> u.sendMessage(message));
        }
    }

    void remove(UserThread user) {
        String username = user.getNickname();
        this.users.remove(user);
        this.privateChatRooms.values().forEach(chatRoom -> chatRoom.removeUser(user));
        notifyAllUsers();
        System.err.println("Client disconnected: " + username);
    }

    List<String> getUserNames() {
        synchronized (this.users) {
            return this.users.stream()
                    .map(UserThread::getNickname)
                    .collect(Collectors.toList());
        }
    }

    UserThread getUserByName(String username) {
        synchronized (this.users) {
            return this.users.stream()
                    .filter(u -> u.getNickname().equals(username))
                    .findFirst()
                    .orElse(null);
        }
    }

    void createPrivateChatRoom(String roomName, UserThread user1, UserThread user2) {
        ChatRoom chatRoom = new ChatRoom(roomName, user1, user2);
        privateChatRooms.put(roomName, chatRoom);
        user1.setCurrentChatRoom(chatRoom);
        user2.setCurrentChatRoom(chatRoom);
    }

    ChatRoom getPrivateChatRoom(String roomName) {
        return privateChatRooms.get(roomName);
    }

    void removePrivateChatRoom(String roomName) {
        privateChatRooms.remove(roomName);
    }

    void handleRequest(String sender, String receiver) {
        UserThread senderThread = getUserByName(sender);
        UserThread receiverThread = getUserByName(receiver);

        if (receiverThread != null) {
            receiverThread.sendMessage("/response " + sender);
        } else {
            senderThread.sendMessage("User not available.");
        }
    }
}
