import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
                } else if (clientMessage.startsWith("/accept")) {
                    handlePrivateChatAcceptance();
                } else if (clientMessage.startsWith("/reject")) {
                    handlePrivateChatRejection();
                } else if (clientMessage.startsWith("/letter ") || clientMessage.startsWith("/word ")) {
                    if (inHangmanGame) {
                        handleHangmanGuess(clientMessage);
                    } else {
                        sendMessage("You are not in a game.");
                    }
                } else if (!inHangmanGame) {
                    if (currentChatRoom == null) {
                        this.server.broadcast(this, "[" + this.username + "]: " + clientMessage);
                    } else {
                        currentChatRoom.broadcast(this, "[" + this.username + "]: " + clientMessage);
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

    private void handlePrivateChatRejection() {
        if (pendingRequestFrom != null) {
            pendingRequestFrom.sendMessage(this.username + " rejected your game request.");
            pendingRequestFrom = null;
        } else {
            sendMessage("No pending game requests.");
        }
    }

    private void handleHangmanGuess(String message) {
        if (message.startsWith("/letter ")) {
            String guess = message.substring(8).trim();
            hangmanGame.guessLetter(this, guess);
        } else if (message.startsWith("/word ")) {
            String guess = message.substring(6).trim();
            hangmanGame.guessWord(this, guess);
        }
    }

    void sendMessage(String message) {
        if (this.toUser != null)
            this.toUser.println(message);
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
}
