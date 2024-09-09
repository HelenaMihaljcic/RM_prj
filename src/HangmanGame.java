import java.util.HashSet;
import java.util.Set;

public class HangmanGame {
    private final UserThread player1;
    private final UserThread player2;
    private final String wordForPlayer1;
    private final String wordForPlayer2;
    private final Set<Character> guessedLettersPlayer1;
    private final Set<Character> guessedLettersPlayer2;
    private boolean player1Turn;
    private boolean gameEnded;
    private String guessedWordPlayer1;
    private String guessedWordPlayer2;
    private final ChatServer server;

    public HangmanGame(UserThread player1, UserThread player2, String word, ChatServer server) {
        this.player1 = player1;
        this.player2 = player2;
        // Assign the same word to both players for simplicity, or use different words if needed
        this.wordForPlayer1 = word.toLowerCase();
        this.wordForPlayer2 = word.toLowerCase();
        this.guessedLettersPlayer1 = new HashSet<>();
        this.guessedLettersPlayer2 = new HashSet<>();
        this.guessedWordPlayer1 = "_".repeat(word.length());
        this.guessedWordPlayer2 = "_".repeat(word.length());
        this.player1Turn = true;
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
        String word = (player == player1) ? wordForPlayer1 : wordForPlayer2;
        Set<Character> guessedLetters = (player == player1) ? guessedLettersPlayer1 : guessedLettersPlayer2;
        String guessedWord = (player == player1) ? guessedWordPlayer1 : guessedWordPlayer2;

        if (word.contains(letter)) {
            guessedLetters.add(letter.charAt(0));
            guessedWord = updateGuessedWord(word, guessedLetters);
            notifyPlayers("correctLetter " + letter + " " + player.getNickname());
            disableLetterButton(player, letter);
        } else {
            notifyPlayers("incorrectLetter " + letter + " " + player.getNickname());
            disableLetterButton(player, letter);
        }

        // Update the guessed word for the current player
        if (player == player1) {
            guessedWordPlayer1 = guessedWord;
        } else {
            guessedWordPlayer2 = guessedWord;
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

        String word = (player == player1) ? wordForPlayer1 : wordForPlayer2;

        if (guess.equalsIgnoreCase(word)) { // Ignore case sensitivity
            gameEnded = true;
            String winner = player.getNickname();
            String loser = (player == player1) ? player2.getNickname() : player1.getNickname();

            notifyPlayers("end WIN " + winner + " " + loser);
        } else {
            notifyPlayers("end LOSS " + player.getNickname() + " " + guess);
            switchTurn();
        }
    }


    private boolean playerTurn(UserThread player) {
        return (player == player1 && player1Turn) || (player == player2 && !player1Turn);
    }

    private void switchTurn() {
        player1Turn = !player1Turn;
        notifyPlayers("/turn :" + (player1Turn ? player1.getNickname() : player2.getNickname()));
    }

    private String updateGuessedWord(String word, Set<Character> guessedLetters) {
        StringBuilder updatedWord = new StringBuilder();
        for (char c : word.toCharArray()) {
            if (c == ' ') {
                updatedWord.append(' '); //razmak za razmake
            } else if (guessedLetters.contains(c)) {
                updatedWord.append(c);
            } else {
                updatedWord.append('_');
            }
        }
        return updatedWord.toString();
    }

    private void checkGameEnd() {
        if (!guessedWordPlayer1.contains("_") || !guessedWordPlayer2.contains("_")) {
            gameEnded = true;
            String winner = "";
            String loser = "";

            if (!guessedWordPlayer1.contains("_")) {
                winner = player1.getNickname();
                loser = player2.getNickname();
            } else if (!guessedWordPlayer2.contains("_")) {
                winner = player2.getNickname();
                loser = player1.getNickname();
            }

            if (!winner.isEmpty()) {
                notifyPlayers("end WIN " + winner + " " + loser);
            } else {
                notifyPlayers("end DRAW");
            }
        }
    }

    private void notifyPlayers(String message) {
        server.sendMessageToUser(player1, message);
        server.sendMessageToUser(player2, message);
    }

    private void disableLetterButton(UserThread player, String letter) {
        String message = "disableButton " + letter.toUpperCase();
        server.sendMessageToUser(player, message);
    }




}
