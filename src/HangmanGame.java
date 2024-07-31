
import java.util.Random;

public class HangmanGame {
    private static final String[] words = {"vanja", "sanja"};
    private static final int MAX_ATTEMPTS = 7;

    private final String word;
    private String asterisk;
    private int attempts;
    private final UserThread player1;
    private final UserThread player2;
    private UserThread currentPlayer;

    public HangmanGame(UserThread player1, UserThread player2) {
        this.word = words[new Random().nextInt(words.length)];
        this.asterisk = new String(new char[word.length()]).replace("\0", "*");
        this.attempts = 0;
        this.player1 = player1;
        this.player2 = player2;
        this.currentPlayer = player1;
    }

    public void startGame() {
        player1.sendMessage("Game started! The word is: " + asterisk);
        player2.sendMessage("Game started! The word is: " + asterisk);
        currentPlayer.sendMessage("It's your turn to guess a letter or word.");
    }

    public void guessLetter(UserThread player, String guess) {
        if (player != currentPlayer) {
            player.sendMessage("It's not your turn!");
            return;
        }

        String newAsterisk = "";
        for (int i = 0; i < word.length(); i++) {
            if (word.charAt(i) == guess.charAt(0)) {
                newAsterisk += guess.charAt(0);
            } else if (asterisk.charAt(i) != '*') {
                newAsterisk += word.charAt(i);
            } else {
                newAsterisk += "*";
            }
        }

        if (asterisk.equals(newAsterisk)) {
            attempts++;
            hangmanImage();
        } else {
            asterisk = newAsterisk;
            if (asterisk.equals(word)) {
                player1.sendMessage("Correct! You win! The word was " + word);
                player2.sendMessage("Correct! You win! The word was " + word);
                endGame();
                return;
            }
        }

        if (attempts >= MAX_ATTEMPTS) {
            player1.sendMessage("GAME OVER! The word was " + word);
            player2.sendMessage("GAME OVER! The word was " + word);
            endGame();
            return;
        }

        switchPlayer();
    }

    public void guessWord(UserThread player, String guess) {
        if (player != currentPlayer) {
            player.sendMessage("It's not your turn!");
            return;
        }

        if (guess.equals(word)) {
            player1.sendMessage("Correct! You win! The word was " + word);
            player2.sendMessage("Correct! You win! The word was " + word);
            endGame();
        } else {
            attempts++;
            hangmanImage();
            if (attempts >= MAX_ATTEMPTS) {
                player1.sendMessage("GAME OVER! The word was " + word);
                player2.sendMessage("GAME OVER! The word was " + word);
                endGame();
                return;
            }
            switchPlayer();
        }
    }

    private void switchPlayer() {
        currentPlayer = (currentPlayer == player1) ? player2 : player1;
        currentPlayer.sendMessage("It's your turn to guess a letter or word.");
    }

    private void hangmanImage() {
        String[] hangmanStages = {
                "Wrong guess, try again",
                "Wrong guess, try again\n\n\n\n\n___|___",
                "Wrong guess, try again\n   |\n   |\n   |\n   |\n   |\n   |\n   |\n___|___",
                "Wrong guess, try again\n   ____________\n   |\n   |\n   |\n   |\n   |\n   |\n   |\n___|___",
                "Wrong guess, try again\n   ____________\n   |          _|_\n   |         /   \\\n   |        |     |\n   |         \\_ _/\n   |\n   |\n   |\n___|___",
                "Wrong guess, try again\n   ____________\n   |          _|_\n   |         /   \\\n   |        |     |\n   |         \\_ _/\n   |           |\n   |           |\n   |\n___|___",
                "Wrong guess, try again\n   ____________\n   |          _|_\n   |         /   \\\n   |        |     |\n   |         \\_ _/\n   |           |\n   |           |\n   |          / \\\n___|___      /   \\",
                "GAME OVER!\n   ____________\n   |          _|_\n   |         /   \\\n   |        |     |\n   |         \\_ _/\n   |          _|_\n   |         / | \\\n   |          / \\\n___|___      /   \\"
        };

        player1.sendMessage(hangmanStages[attempts]);
        player2.sendMessage(hangmanStages[attempts]);
    }

    private void endGame() {
        player1.setInHangmanGame(false);
        player2.setInHangmanGame(false);
        player1.setCurrentChatRoom(null);
        player2.setCurrentChatRoom(null);
    }
}
