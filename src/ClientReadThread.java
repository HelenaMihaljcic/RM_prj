import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

final class ClientReadThread extends Thread {
    private BufferedReader fromServer;
    private String username;
    private Main main;
    private Stage primaryStage;
    private String currentWord;

    ClientReadThread(String username, Socket socket, Main main, Stage primaryStage) { // Add primaryStage parameter
        this.username = username;
        this.main = main;
        this.primaryStage = primaryStage; // Initialize the field
        try {
            this.fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException ex) {
            System.out.println("Error getting input stream: " + ex.getMessage());
            ex.printStackTrace();
        }
    }


    @Override
    public void run() {
        while (true) {
            try {
                String response = this.fromServer.readLine();
                if (response == null) {
                    System.err.println("\rConnection lost.");
                    return;
                }

                System.err.println(response);

                if (response.startsWith("UPDATE_USERS")) {
                    String users = response.substring(13);
                    String[] userArray = users.split(",");
                    Platform.runLater(() -> {
                        main.updateUserList(userArray);
                    });
                }else if (response.startsWith("UPDATE_USERS2")) {
                    String users = response.substring(14);
                    String[] userArray = users.split(",");
                    Platform.runLater(() -> {
                        // Update the user list in Main class with users not in a Hangman game
                        main.updateUserList(userArray);
                    });
                }  else if (response.startsWith("/response ")) {
                    String fromUser = response.substring(10);
                    main.receivedRequest(fromUser);
                } else if (response.startsWith("REQUEST_DECLINED")) {
                    main.requestDeclined();
                } else if (response.startsWith("REQUEST_ACCEPTED")) {
                    String[] parts = response.split(":");
                    if (parts.length == 4) {
                        String category = parts[1];
                        String word = parts[2];
                        String startPlayer = parts[3];
                        Platform.runLater(() -> {
                            try {
                                main.switchToGameScene(category, word, startPlayer);
                                this.currentWord = word;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                } else if (response.startsWith("/turn")) {
                    String[] players = response.split(":");
                    String currentPlayer = players[1];

                    Platform.runLater(() -> {
                        main.setTurnLabel(currentPlayer);
                    });
                } else if (response.contains("NOT TURN")) {
                    main.showNotYourTurnAlert();
                } else if (response.startsWith("/MSG")) {
                    String[] msg = response.split(":");
                    String player = msg[1];
                    String message = msg[2];

                    if (message.startsWith("/word ")) {
                        String guessedWord = message.substring(6).trim();
                        Platform.runLater(() -> {
                            if (guessedWord.equalsIgnoreCase(currentWord)) {
                                main.updateWordDisplay2(guessedWord);

                                if (player.equals(main.getUsername())) {
                                    int points = guessedWord.length() * 10;
                                    main.updateScore(player, points);
                                    main.showEndMessage(player, main.getPlayerScore(player));
                                }
                                main.updateChat("Player " + player + " guessed the word: " + guessedWord);
                            } else {
                                if (player.equals(main.getUsername())) {
                                    main.showIncorrectGuessMessage(guessedWord);
                                }
                                main.updateChat("Player " + player + " sent the wrong word: " + guessedWord);
                            }
                        });
                    } else {
                        Platform.runLater(() -> main.updateChat("[" + player + "]: " + message));
                    }
                }else if (response.startsWith("disableButton ")) {
                    String[] parts = response.split(" ");
                    String letter = parts[1];
                    Platform.runLater(() -> {
                        Button button = main.getButtonById(letter);
                        if (button != null) {
                            button.setDisable(true);
                        }
                    });
                } else if (response.startsWith("correctLetter ")) {
                    String[] parts = response.split(" ");
                    String player = parts[2];
                    String letter = parts[1];

                    Platform.runLater(() -> {
                        if (player.equals(main.getUsername())) {
                            main.updateWordDisplay(letter);
                        }
                    });
                }  else if (response.startsWith("incorrectLetter ")) {
                    String[] parts = response.split(" ");
                    String player = parts[2];
                    String letter = parts[1];

                    Platform.runLater(() -> {
                        if (player.equals(main.getUsername())) {
                            main.showIncorrectGuessMessage(letter);
                            main.updateScore(player, -5);  // Oduzmite 5 poena za pogrešan odgovor
                        }
                    });
                } else if (response.startsWith("end WIN ")) {

                String[] parts = response.split(" ");

                if (parts.length == 4) {
                    String winner = parts[2];
                    int winnerScore = main.getPlayerScore(winner);

                    Platform.runLater(() -> main.showEndMessage(winner, winnerScore));

                } else {
                    System.err.println("Greška u formatu poruke o kraju igre.");
                }
            }
            else {
                    System.out.println("\r" + response);
                    System.out.printf("\r[%s]: ", this.username);
                }
            } catch (IOException ex) {
                System.out.println("Error reading from server: " + ex.getMessage());
                ex.printStackTrace();
                break;
            }
        }
    }


}
