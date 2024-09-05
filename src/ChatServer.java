import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

final class ChatServer {
    static final int SERVER_TEST_PORT = 12345;
    private static Map<String, List<String>> kategorijeMap = new HashMap<>();
    private final Map<String, HangmanGame> activeGames = new HashMap<>();
    private static BufferedReader fromUser;
    private static PrintWriter toUser;
    private static ChatServer server = new ChatServer(SERVER_TEST_PORT);
    private final int port;
    private  Set<UserThread> users;
    private Map<String, ChatRoom> privateChatRooms = new HashMap<>();

    public static void main(String[] args) {

        if(server == null){
            System.out.println("JOOOOO");
        }

        server.execute();
    }



    ChatServer(int port) {
        this.port = port;
        this.users = Collections.synchronizedSet(new HashSet<>());
    }


    void execute() {
        try (ServerSocket server = new ServerSocket(port)) {
            System.err.println("Chat server is listening on port: " + port);

            loadCategories("Kategorije");

            while (true) {
                Socket client = server.accept();

                fromUser = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String username = fromUser.readLine();
                System.out.println(username + " connected.");


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

    public void broadcast(UserThread sender, String message) {
        synchronized (this.users) {
            this.users.stream()
                    .filter(u -> u != sender)
                    .forEach(u -> u.getToUser().println(message));
        }
    }



    void remove(UserThread user) {
        String username = user.getNickname();
        this.users.remove(user);
        privateChatRooms.values().forEach(chatRoom -> chatRoom.removeUser(user));
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
        ChatRoom chatRoom = new ChatRoom(roomName, user1, user2, this);
        privateChatRooms.put(roomName, chatRoom);
        System.out.println(privateChatRooms);
        user1.setCurrentChatRoom(chatRoom);
        user2.setCurrentChatRoom(chatRoom);
    }

    void deletePrivateChatRoom(String roomName) {
        // Synchronized block to ensure thread safety while modifying the map
        synchronized (privateChatRooms) {
            // Iterate through the keys in the map
            Iterator<String> iterator = privateChatRooms.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                // Check if the key (room name) contains the specified roomName
                if (key.contains(roomName)) {
                    // Remove the entry if it matches the condition
                    iterator.remove();
                    break; // Exit the loop once the room is found and removed
                }
            }
        }
    }


    public static String[] getRandomCategoryAndWord() {
        if (kategorijeMap.isEmpty()) {
            return null; // Nema kategorija
        }

        // Nasumično izaberi kategoriju
        List<String> categories = new ArrayList<>(kategorijeMap.keySet());
        String randomCategory = categories.get(new Random().nextInt(categories.size()));

        // Nasumično izaberi reč iz izabrane kategorije
        List<String> words = kategorijeMap.get(randomCategory);
        String randomWord = words.get(new Random().nextInt(words.size()));

        return new String[]{randomCategory, randomWord};
    }

    private static void loadCategories(String folderPath) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(folderPath), "*.txt")) {
            for (Path entry : stream) {
                String categoryName = entry.getFileName().toString().replace(".txt", "");
                List<String> words = Files.lines(entry)
                        .collect(Collectors.toList());
                kategorijeMap.put(categoryName, words);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
    private HangmanGame hangmanGame;

    void handleGameAcceptance(String sender, String receiver) {
        String[] challenge = getRandomCategoryAndWord();
        String category = challenge[0];
        String word = challenge[1];

        UserThread senderThread = getUserByName(sender);
        UserThread receiverThread = getUserByName(receiver);

        if (senderThread != null && receiverThread != null) {
            hangmanGame = new HangmanGame(senderThread, receiverThread, word, this);
            createPrivateChatRoom(sender + ":" + receiver, senderThread, receiverThread);

            System.out.println(privateChatRooms);
            senderThread.setHangmanGame(hangmanGame);
            receiverThread.setHangmanGame(hangmanGame);
            senderThread.setInHangmanGame(true);
            receiverThread.setInHangmanGame(true);

            // Notify both users about the game start
            String gameStartMessage = "REQUEST_ACCEPTED:" + category + ":" + word + ":" + senderThread.getNickname();
            senderThread.sendMessage(gameStartMessage);
            receiverThread.sendMessage(gameStartMessage);
        }
    }

    public synchronized ChatRoom findChatRoomByPlayer(String playerName) {
        for (Map.Entry<String, ChatRoom> entry : privateChatRooms.entrySet()) {
            String[] players = entry.getKey().split(":");
            if (players[0].equals(playerName) || players[1].equals(playerName)) {
                return entry.getValue();
            }
        }
        return null; // Ako nije pronađena nijedna soba za tog igrača
    }

    // Metoda za slanje poruka u sobu
    public synchronized void handleSendMessageToRoom(String message, String playerName) {
        ChatRoom room = findChatRoomByPlayer(playerName);
        if (room != null) {
            room.broadcastRoom(playerName, message);
        } else {
            System.err.println("Chat room not found for player: " + playerName);
        }
    }



    public void handleHangmanGuess(String message, UserThread sender) {
        if (message.startsWith("/letter ")) {
            String guess = message.substring(8).trim();
            //System.out.println(guess); dobro dodje serveru!
            hangmanGame.guessLetter(sender, guess);
        } else if (message.startsWith("/word ")) {
            String guess = message.substring(6).trim();
            hangmanGame.guessWord(sender, guess);
        }
    }

    public void sendMessageToUser(UserThread player1, String message) {
        player1.getToUser().println(message);

    }

    public static synchronized ChatServer returnInstance() {
        return server;
    }

    public Map<String, ChatRoom> getPrivateChatRooms() {
        System.out.println(privateChatRooms);
        return privateChatRooms;
    }
}
