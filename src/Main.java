import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.QuadCurve;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;

public class Main extends Application {
    public Button unesiIme;
    private String word;

    @FXML
    private ListView<String> igraciLV; // za prikazivanje igraca na pocetku
    @FXML
    private Button pokreniDugme, chatButton;
    @FXML
    private ListView<String> chatLV;  // za prikazivanje chata
    @FXML
    private TextField chatTF, nameTF;  // za unos poruke u chat
    @FXML
    private Label labelName, labelCategory, labelScore, labelTurn; //za popunjavanje kad se pokrene igra
    @FXML
    private AnchorPane hangmanAP, wordsAP;
    @FXML
    private Label crticaLabel;
    @FXML
    private Line lijevaNoga, desnaNoga, lijevaRuka, desnaRuka, tijelo, nos;
    @FXML
    private Circle glava, lijevoOko, desnoOko;
    @FXML
    private QuadCurve usne;
    @FXML
    private Button A, B, C, Č, Ć, D, Dž, Đ, E, F, G, H, I, J, K, L, Lj, M, N, Nj, O, P, R, S, Š, T, U, V, Z, Ž;
    private Stage primaryStage;
    private ChatClient chatClient;
    private Parent root;
    private String guessedLetter;
    private String playerName;
    private static ChatServer chatServer;
    private Map<String, Label> crticeMap = new HashMap<>();
    private int incorrectAttempts = 0;
    private static final int MAX_ATTEMPTS = 10;

    private int score = 0;
    private Map<String, Integer> playerScores = new HashMap<>();
    private Map<String, Label> playerScoreLabels = new HashMap<>();



    public String getUsername() {
        return this.nameTF.getText().trim();
    }


    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;
        stage.setTitle("HANGMAN");

        // Load the initial scene
        root = FXMLLoader.load(getClass().getResource("Scene/pocetna_scena.fxml"));
        Scene scene = new Scene(root, 850, 600);
        stage.setScene(scene);

        stage.show();


        primaryStage.setOnCloseRequest(event -> {
            ChatClient chatClient = ChatClientManager.getInstance();
            if (chatClient != null && chatClient.isAlive()) {
                chatClient.shutdown();
            }
            System.exit(0);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }


    public void switchToGameScene(String category, String word, String player) throws IOException {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("Scene/glavna_scena.fxml"));
            Scene gameScene = new Scene(root);
            primaryStage = (Stage) igraciLV.getScene().getWindow();
            primaryStage.setScene(gameScene);
            primaryStage.show();
            startGame(word, category, player);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    public void startGame(String word, String category, String startPlayer) {
        Scene currentScene = primaryStage.getScene();
        if (currentScene == null) {
            System.err.println("Error: currentScene is null");
            return;
        }

        this.word = word.toLowerCase();

        wordsAP = (AnchorPane) currentScene.lookup("#wordsAP");
        crticaLabel = (Label) currentScene.lookup("#crticaLabel");
        Label labelCategory = (Label) currentScene.lookup("#labelCategory");
        labelTurn = (Label) currentScene.lookup("#labelTurn");
        labelName = (Label) currentScene.lookup("#labelName");
        glava = (Circle) currentScene.lookup("#glava");
        tijelo = (Line) currentScene.lookup("#tijelo");
        lijevaRuka = (Line) currentScene.lookup("#lijevaRuka");
        desnaRuka = (Line) currentScene.lookup("#desnaRuka");
        lijevaNoga = (Line) currentScene.lookup("#lijevaNoga");
        desnaNoga = (Line) currentScene.lookup("#desnaNoga");
        lijevoOko = (Circle) currentScene.lookup("#lijevoOko");
        desnoOko = (Circle) currentScene.lookup("#desnoOko");
        nos = (Line) currentScene.lookup("#nos");
        usne = (QuadCurve) currentScene.lookup("#usne");


        if (labelCategory != null) {
            labelCategory.setText(category);
        } else {
            System.out.println("Labela za kategoriju nije pronađena na sceni.");
        }
        if(labelTurn != null){
            labelTurn.setText(startPlayer);
        }
        if(labelName != null){
            labelName.setText(playerName);
        }

        System.out.println("Kategorija: " + category + " rijec: " + word);

        if (wordsAP != null && crticaLabel != null) {
            wordsAP.getChildren().clear();
            crticeMap.clear();

            double spacing = 40;
            double initialLayoutY = crticaLabel.getLayoutY();

            String[] words = word.split(" ");
            double layoutY = initialLayoutY;

            for (String singleWord : words) {
                double totalWidth = singleWord.length() * spacing;
                double layoutX = (wordsAP.getWidth() - totalWidth) / 2; // Centriraj crtice u svakom redu

                for (int i = 0; i < singleWord.length(); i++) {
                    Label dash = new Label("_");
                    dash.setLayoutX(layoutX + i * spacing);
                    dash.setLayoutY(layoutY);
                    dash.setStyle("-fx-font-size: 40px; -fx-text-fill: white; -fx-font-family: 'Dialog'; -fx-alignment: center;");
                    dash.setVisible(true);
                    wordsAP.getChildren().add(dash);

                    // Koristimo format "red, pozicija u redu" kao ključ
                    crticeMap.put(layoutY + "," + (layoutX + i * spacing), dash);
                }
                // Prelazi u novi red za svaku reč
                layoutY += 35;
            }

            List<String> players = new ArrayList<>(Arrays.asList(igraciLV.getItems().toArray(new String[0])));
            for (String player : players) {
                if (!playerScoreLabels.containsKey(player)) {
                    // Ako labela za igrača ne postoji, pronađi je i dodaj u mapu
                    Label scoreLabel = (Label) currentScene.lookup("#" + player + "Score");
                    if (scoreLabel != null) {
                        playerScoreLabels.put(player, scoreLabel);
                        playerScores.put(player, 0); // Početni score
                    }
                }
            }


        } else {
            System.out.println("AnchorPane za crtice ili template labela nisu pronađeni na sceni.");
        }
    }


    @FXML
    public void ConnectUser(ActionEvent event) {
        this.playerName = nameTF.getText().trim();
        this.chatClient = new ChatClient("localhost", 12345, this, playerName);
        ChatClientManager.setInstance(chatClient);
        this.chatClient.start();

        unesiIme.setDisable(true);
    }

    public void updateUserList(String[] users) {
        Platform.runLater(() -> {
            igraciLV.getItems().clear();
            igraciLV.getItems().addAll(users);
        });
    }




    public void receivedRequest(String fromUser) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Game Request");
            alert.setHeaderText("Game Request from " + fromUser);
            alert.setContentText("Do you want to accept the game request?");

            ButtonType acceptButton = new ButtonType("Accept");
            ButtonType declineButton = new ButtonType("Decline");

            alert.getButtonTypes().setAll(acceptButton, declineButton);
            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent() && result.get() == acceptButton) {
                chatClient.sendMessage("REQUEST_ACCEPTED " + fromUser);

            } else {
                chatClient.sendMessage("REQUEST_DECLINED " + fromUser);
            }
        });
    }

    @FXML
    public void handlePlayGame(ActionEvent event) {
        String selectedUser = igraciLV.getSelectionModel().getSelectedItem();
        String currentUser = getUsername();

        if (selectedUser != null && !selectedUser.equals(currentUser)) {
            chatClient.sendMessage("/request " + selectedUser);
        } else if (selectedUser != null && selectedUser.equals(currentUser)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Invalid Selection");
            alert.setHeaderText(null);
            alert.setContentText("You cannot send a request to yourself.");
            alert.showAndWait();
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No User Selected");
            alert.setHeaderText(null);
            alert.setContentText("Please select a user to play with.");
            alert.showAndWait();
        }
    }


    public void requestDeclined() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Request Declined");
            alert.setHeaderText(null);
            alert.setContentText("Your game request was declined.");
            alert.showAndWait();
        });
    }


    public void sendMessageGUI(ActionEvent event){

        Scene currentScene = labelTurn.getScene();

        chatTF = (TextField) currentScene.lookup("#nameTF");
        String message = nameTF.getText();

        ChatRoom room = chatServer.getPrivateChatRoom(this.playerName);
        room.broadcast(this.playerName, message);
    }

    public String returnGuessedLetter() {
        return guessedLetter;
    }

    public void setGuessedLetter(String guessedLetter) {
        this.guessedLetter = guessedLetter;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    @FXML
    private void handleLetterGuess(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        String guessedLetter = clickedButton.getText();
        ChatClientManager.getInstance().sendMessage("/letter " + guessedLetter); //salje serveru!
    }

    public void updateWordDisplay(String guessedLetter) {
        char guessedChar = guessedLetter.charAt(0);
        int occurrences = 0;

        // Prođi kroz reč i ažuriraj labelu za svako pojavljivanje slova
        for (int i = 0; i < word.length(); i++) {
            char currentChar = word.charAt(i);
            if (currentChar == guessedChar) {
                String key = findLabelKey(i);
                if (key != null) {
                    Label label = crticeMap.get(key);
                    label.setText(String.valueOf(guessedChar));
                    occurrences++;
                }
            }
        }

        if (occurrences > 0) {
            updateScore(getUsername(), 10 * occurrences);  // Dodaj 10 poena za svako pojavljivanje slova
        }
    }


    private String findLabelKey(int index) {
        double spacing = 40;
        double layoutY = crticaLabel.getLayoutY();
        int wordIndex = 0;

        for (String singleWord : word.split(" ")) {
            if (index >= wordIndex && index < wordIndex + singleWord.length()) {
                double layoutX = (wordsAP.getWidth() - singleWord.length() * spacing) / 2;
                return layoutY + "," + (layoutX + (index - wordIndex) * spacing);
            }
            wordIndex += singleWord.length() + 1; // +1 za razmak
            layoutY += 35;
        }
        return null;
    }

    private void updateHangmanDisplay() {
        switch (incorrectAttempts) {
            case 1:
                glava.setVisible(true);
                break;
            case 2:
                tijelo.setVisible(true);
                break;
            case 3:
                lijevaRuka.setVisible(true);
                break;
            case 4:
                desnaRuka.setVisible(true);
                break;
            case 5:
                lijevaNoga.setVisible(true);
                break;
            case 6:
                desnaNoga.setVisible(true);
                break;
            case 7:
                lijevoOko.setVisible(true);
                break;
            case 8:
                desnoOko.setVisible(true);
                break;
            case 9:
                nos.setVisible(true);
                break;
            case 10:
                usne.setVisible(true);
                break;
            default:
                break;
        }
    }


    public void showEndMessage(String winner, int score){
        Platform.runLater(() -> {
            String message;
            if (winner.equals(getUsername())) {
                message = "CONGRATULATIONS! You won! Your score is " + score;
            } else {
                message = "GOOD GAME! The winner is " + winner.toUpperCase() + ".";
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
            alert.setTitle("End of the Game");
            alert.setHeaderText(null);
            alert.showAndWait();
        });
    }

    private void showEndMessage2(){
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Oops, game over! You lost!", ButtonType.OK);
        alert.setTitle("End of the Game");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public void showIncorrectGuessMessage(String letter) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Incorrect guess: " + letter, ButtonType.OK);
            alert.setTitle("Incorrect Guess");
            alert.setHeaderText(null);
            alert.showAndWait();

            incorrectAttempts++;
            updateHangmanDisplay();

            if (incorrectAttempts >= MAX_ATTEMPTS) {
                showEndMessage2();
            }
        });
    }



    public void setTurnLabel(String message){
        labelTurn.setText(message);
    }



    public void showNotYourTurnAlert() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING, "It's not your turn!", ButtonType.OK);
            alert.setTitle("Turn Notification");
            alert.setHeaderText(null);
            alert.showAndWait();
        });
    }

    public void updateChat(String chatMessage) {
        Platform.runLater(() -> {
            chatLV.getItems().add(chatMessage);
        });
    }


    Button getButtonById(String id) {
        Button dugme =  (Button) primaryStage.getScene().lookup("#" + id);
        dugme.setDisable(true);
        return dugme;
    }

    public void updateScore(String player, int scoreChange) {
        Scene currentScene = primaryStage.getScene();

        Label scoreLabel = (Label) currentScene.lookup("#labelScore");

        if (scoreLabel != null) {
            String text = scoreLabel.getText();
            int currentScore = 0;
            try {
                currentScore = Integer.parseInt(text.trim());
            } catch (NumberFormatException e) {
                System.err.println("Greška u parsiranju trenutnog rezultata: " + e.getMessage());
            }

            int newScore = currentScore + scoreChange;
            playerScores.put(player, newScore);

            Platform.runLater(() -> scoreLabel.setText(String.valueOf(newScore)));
        } else {
            System.err.println("Labela za rezultat nije pronađena u trenutnoj sceni.");
        }
    }
    public int getPlayerScore(String playerName) {
        return playerScores.getOrDefault(playerName, 0);
    }



}
