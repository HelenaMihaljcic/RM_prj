import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
    private final Map<String, HangmanGame> activeGames = new HashMap<>(); // Dodata promenljiva za aktivne igre

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

            loadCategories("Kategorije");

            while (true) {
                Socket client = server.accept();

                BufferedReader fromUser = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String username = fromUser.readLine();
                System.out.println(username + " connected.");

                // Kreirajte novu instancu UserThread za svakog korisnika
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
            // Create the game and start it
            senderThread.setInHangmanGame(true);
            receiverThread.setInHangmanGame(true);

            // Notify both users about the game start
            String gameStartMessage = "REQUEST_ACCEPTED:" + category + ":" + word;
            senderThread.sendMessage(gameStartMessage);
            receiverThread.sendMessage(gameStartMessage);
        }
    }

    private void handleHangmanGuess2(String message) {
        String guessedLetter = message.substring(8).trim();
        UserThread sender = getUserByName(message.split(" ")[1]);

        if (sender != null) {
            sender.sendMessage("LETTER " + guessedLetter);
        }
    }

    public void handleHangmanGuess(String message, UserThread sender) {
        if (message.startsWith("/letter ")) {
            String guess = message.substring(8).trim();
            System.out.println(guess); //dobro dodje serveru!
            hangmanGame.guessLetter(sender, guess);
            /*
            if (correct) {
                sender.sendMessage("Correct guess!");
            } else {
                sender.sendMessage("Incorrect guess. Try again.");
            }

             */
        } else if (message.startsWith("/word ")) {
            String guess = message.substring(6).trim();
            hangmanGame.guessWord(sender, guess);
            /*
            if (correct) {
                sender.sendMessage("Congratulations, you guessed the word!");
            } else {
                sender.sendMessage("Wrong word. Keep guessing.");
            }

             */
        }
    }

    public void sendMessageToUser(UserThread player1, String message) {
        player1.getToUser().println(message);

    }
}
