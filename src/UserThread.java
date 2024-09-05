import java.io.*;
import java.net.Socket;

final class UserThread extends Thread {
    private final ChatServer server;
    private final Socket sock;
    private BufferedReader fromUser;
    private PrintWriter toUser;
    private String username;
    private UserThread pendingRequestFrom;
    private HangmanGame hangmanGame;
    private boolean inHangmanGame = false;
    private ChatRoom currentChatRoom;

    UserThread(Socket socket, ChatServer server, String username) {
        this.sock = socket;
        this.server = server;
        try {
            this.fromUser = new BufferedReader(new InputStreamReader(this.sock.getInputStream()));
            this.toUser = new PrintWriter(this.sock.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.username = username;
    }


    @Override
    public void run() {
        try {
            this.username = fromUser.readLine();
            this.sendMessage("Connected users: " + this.server.getUserNames());

            this.server.broadcast(this, "New user connected: " + this.username);

            String clientMessage;
            do {
                clientMessage = fromUser.readLine();
                if (clientMessage == null)
                    break;

                if (clientMessage.startsWith("/request ")) {
                    String targetUsername = clientMessage.substring(9).trim();
                    server.handleRequest(this.username, targetUsername);
                } else if (clientMessage.startsWith("/response ")) {
                    String receivedUsername = clientMessage.substring(10).trim();
                    handlePrivateChatRequest(receivedUsername);
                } else if (clientMessage.contains("REQUEST_ACCEPTED")) {
                    String targetUsername = clientMessage.substring(17).trim();

                    server.handleGameAcceptance(this.username, targetUsername);
                } else if (clientMessage.startsWith("/reject")) {
                    handlePrivateChatRejection();
                } else if (clientMessage.startsWith("/msg")) {
                    String parts[] = clientMessage.split(":");
                    server.handleSendMessageToRoom(parts[2].trim(), parts[1].trim());
                } else if (clientMessage.startsWith("/letter ") || clientMessage.startsWith("/word ")) {
                    if (inHangmanGame) {
                        server.handleHangmanGuess(clientMessage, this);
                    } else {
                        sendMessage("You are not in a game.");
                    }
                }
            } while (!clientMessage.equals("bye"));

            this.server.broadcast(this, this.username + " has left the chat.");
        } catch (IOException ex) {
            System.out.println("Error in UserThread: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            this.server.remove(this);

            try {
                this.sock.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handlePrivateChatRequest(String targetUsername) {
        UserThread targetUser = server.getUserByName(targetUsername);
        if (targetUser != null) {
            targetUser.sendMessage(this.username + " wants to start a Hangman game with you. Type /accept to accept or /reject to reject.");
            targetUser.setPendingRequestFrom(this);
        } else {
            sendMessage("User " + targetUsername + " not found.");
        }
    }

    /*
    private void handlePrivateChatAcceptance() {
        if (pendingRequestFrom != null) {
            this.inHangmanGame = true;
            pendingRequestFrom.inHangmanGame = true;
            this.hangmanGame = new HangmanGame(this, pendingRequestFrom);
            pendingRequestFrom.hangmanGame = this.hangmanGame;
            this.hangmanGame.startGame();

            // Notify both users about the game start
            this.sendMessage("GAME_STARTED");
            pendingRequestFrom.sendMessage("GAME_STARTED");

            pendingRequestFrom = null;
        } else {
            sendMessage("No pending game requests.");
        }
    }



     */


    private void handlePrivateChatRejection() {
        if (pendingRequestFrom != null) {
            pendingRequestFrom.sendMessage(this.username + " rejected your game request.");
            pendingRequestFrom = null;
        } else {
            sendMessage("No pending game requests.");
        }
    }
    /*

    private void handleHangmanGuess(String message) {
        if (message.startsWith("/letter ")) {
            String guess = message.substring(8).trim();
            hangmanGame.guessLetter(this, guess);
        } else if (message.startsWith("/word ")) {
            String guess = message.substring(6).trim();
            hangmanGame.guessWord(this, guess);
        }
    }

     */

    void sendMessage(String message) {
        if (this.toUser != null)
            this.toUser.println(message);
    }
    public void sendToServer(String message){

    }

    String getNickname() {
        return this.username;
    }

    void setPendingRequestFrom(UserThread user) {
        this.pendingRequestFrom = user;
    }

    void setCurrentChatRoom(ChatRoom chatRoom) {
        this.currentChatRoom = chatRoom;
    }

    public void setInHangmanGame(boolean inHangmanGame) {
        this.inHangmanGame = inHangmanGame;
    }


    public ChatServer getServer() {
        return server;
    }

    public Socket getSock() {
        return sock;
    }

    public BufferedReader getFromUser() {
        return fromUser;
    }

    public PrintWriter getToUser() {
        return toUser;
    }

    public String getUsername() {
        return username;
    }

    public UserThread getPendingRequestFrom() {
        return pendingRequestFrom;
    }

    public HangmanGame getHangmanGame() {
        return hangmanGame;
    }

    public boolean isInHangmanGame() {
        return inHangmanGame;
    }

    public ChatRoom getCurrentChatRoom() {
        return currentChatRoom;
    }

    public void setHangmanGame(HangmanGame hangmanGame) {
        this.hangmanGame = hangmanGame;
    }
}
