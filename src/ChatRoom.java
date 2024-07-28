package p02_chat;

import java.util.HashSet;
import java.util.Set;

public class ChatRoom {
    private final String roomName;
    private final Set<UserThread> users;

    public ChatRoom(String roomName, UserThread user1, UserThread user2) {
        this.roomName = roomName;
        this.users = new HashSet<>();
        this.users.add(user1);
        this.users.add(user2);
    }

    public void broadcast(UserThread sender, String message) {
        synchronized (this.users) {
            this.users.stream()
                    .filter(u -> u != sender)
                    .forEach(u -> u.sendMessage(message));
        }
    }

    public void removeUser(UserThread user) {
        this.users.remove(user);
    }
}

