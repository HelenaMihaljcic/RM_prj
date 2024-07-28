package p02_chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Set;

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

    UserThread(Socket socket, ChatServer server) {
        this.sock = socket;
        this.server = server;
        try {
            this.fromUser = new BufferedReader(new InputStreamReader(this.sock.getInputStream()));
            this.toUser = new PrintWriter(this.sock.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            // Upon connecting, read username and send connected users list
            this.username = fromUser.readLine();
            this.sendMessage("Connected users: " + this.server.getUserNames());

            // Broadcast that new user has entered the chat
            this.server.broadcast(this, "New user connected: " + this.username);

            // Process the user (until he leaves the chat)
            String clientMessage;
            do {
                // Read message from user
                clientMessage = fromUser.readLine();
                if (clientMessage == null)
                    break;

                // Handle private chat request
                if (clientMessage.startsWith("/request ")) {
                    handlePrivateChatRequest(clientMessage.substring(9).trim());
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

            // Broadcast that user has disconnected
            this.server.broadcast(this, this.username + " has left the chat.");
        } catch (IOException ex) {
            System.out.println("Error in UserThread: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            // Remove user from set
            this.server.remove(this);

            // Close socket
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
