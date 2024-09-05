public class ChatRoom {
    private ChatServer server;
    private final String roomName;
    private UserThread userOne, userTwo;

    public ChatRoom(String roomName, UserThread user1, UserThread user2, ChatServer server) {
        this.roomName = roomName;
        this.userOne = user1;
        this.userTwo = user2;
        this.server = server;
    }

    public void broadcastRoom(String sender, String message) {

            server.sendMessageToUser(userTwo, "/MSG:" + sender + ":" + message);
            server.sendMessageToUser(userOne, "/MSG:" + sender + ":" + message);
    }

    public void removeUser(UserThread user){
        if(userOne.getNickname().equals(user.getNickname())){
            userOne = null;
        }else{
            userTwo = null;
        }
    }

    public String getRoomName() {
        return roomName;
    }
}

