
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class HangmanGame { private final UserThread player1;
    private final UserThread player2;
    private final String word;
    private final Set<String> guessedLetters;
    private boolean player1Turn;
    private boolean gameEnded;
    private String guessedWord;
    private final ChatServer server;

    public HangmanGame(UserThread player1, UserThread player2, String word, ChatServer server) {
        this.player1 = player1;
        this.player2 = player2;
        this.word = word.toLowerCase();
        this.guessedLetters = new HashSet<>();
        this.guessedWord = "_".repeat(word.length());
        this.player1Turn = true; // player1 starts
        this.gameEnded = false;
        this.server = server;
    }

    public void guessLetter(UserThread player, String letter) {
        if (!playerTurn(player)) {
            server.sendMessageToUser(player, "NOT TURN");
            return;
        }

        if (gameEnded) {
            server.sendMessageToUser(player, "END");
            return;
        }

        letter = letter.toLowerCase();


        if (word.indexOf(letter) >= 0) {
            guessedLetters.add(letter);
            updateGuessedWord();
            notifyPlayers("correctLetter " + letter);
        } else {
            notifyPlayers("incorrectLetter " + letter);
        }

        checkGameEnd();
        switchTurn();
    }

    public void guessWord(UserThread player, String guess) {
        if (!playerTurn(player)) {
            player.sendMessage("It's not your turn.");
            return;
        }

        if (gameEnded) {
            player.sendMessage("The game has already ended.");
            return;
        }

        if (guess.equals(word)) {
            gameEnded = true;
            notifyPlayers("end WIN");
        } else {
            notifyPlayers("end LOSS");
        }
    }

    private boolean playerTurn(UserThread player) {
        return (player == player1 && player1Turn) || (player == player2 && !player1Turn);
    }

    private void switchTurn() {
        player1Turn = !player1Turn;
        notifyPlayers("/turn :" + (player1Turn ? player1.getNickname() : player2.getNickname()));
    }

    private void updateGuessedWord() {
        StringBuilder updatedWord = new StringBuilder();
        for (char c : word.toCharArray()) {
            if (guessedLetters.contains(c)) {
                updatedWord.append(c);
            } else {
                updatedWord.append("_");
            }
        }
        guessedWord = updatedWord.toString();
    }

    private void checkGameEnd() {
        if (!guessedWord.contains("_")) {
            gameEnded = true;
            notifyPlayers("end WIN");
        }
    }

    private void notifyPlayers(String message) {
        server.sendMessageToUser(player1, message);
        server.sendMessageToUser(player2, message);

    }
}
