import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.QuadCurve;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.PipedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Main extends Application {
    private static final String FILE_NAME = "data.txt";
    public Button unesiIme;
    private String word, category;
    private List<Label> listaCrtica = new ArrayList<>();

    @FXML
    private ListView<String> igraciLV; // za prikazivanje igraca na pocetku
    @FXML
    private Button pokreniDugme, chatButton;
    @FXML
    private ListView<String> chatLV;  // za prikazivanje chata
    @FXML
    private TextField chatTF, nameTF;  // za unos poruke u chat
    @FXML
    private Label labelName, labelCategory, labelScore; //za popunjavanje kad se pokrene igra
    @FXML
    private AnchorPane hangmanAP, wordsAP; //anchor za covjeka - napraviti transparentno, wordsAP za crtice
    @FXML
    private Label crticaLabel;
    @FXML
    private Line lijevaNoga, desnaNoga, lijevaRuka, desnaRuka, tijelo, nos;
    @FXML
    private Circle glava, lijevoOko, desnoOko;
    @FXML
    private QuadCurve usne;
    private static CategoryWords categoryWords;
    private Scene pocetna;
    private Stage primaryStage;
    private ChatClient chatClient, currentClient;
    private Parent root;


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
        //categoryWords = new CategoryWords(FILE_NAME);
        launch(args);
    }


    public void switchToGameScene(String category, String word) throws IOException {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("Scene/glavna_scena.fxml"));
            Scene gameScene = new Scene(root);
            primaryStage = (Stage) igraciLV.getScene().getWindow();
            primaryStage.setScene(gameScene);
            primaryStage.show();
            startGame(word, category);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    public void startGame(String word, String category) {


        Scene currentScene = primaryStage.getScene();
        if (currentScene == null) {
            System.err.println("Error: currentScene is null");
            return;
        }

        AnchorPane wordsAP = (AnchorPane) currentScene.lookup("#wordsAP");
        Label crticaLabel = (Label) currentScene.lookup("#crticaLabel");
        Label labelCategory = (Label) currentScene.lookup("#labelCategory");

        if (labelCategory != null) {
            labelCategory.setText(category);
        } else {
            System.out.println("Labela za kategoriju nije pronađena na sceni.");
        }

        System.out.println("Kategorija: " + category + " rijec: " + word);


        if (wordsAP != null && crticaLabel != null) {
            wordsAP.getChildren().clear();
            listaCrtica.clear();

            double spacing = 40; // Razmak između crtica
            double totalWidth = word.length() * spacing; // Ukupna širina crtica

            double layoutX = (wordsAP.getWidth() - totalWidth) / 2;
            double layoutY = crticaLabel.getLayoutY();

            for (int i = 0; i < word.length(); i++) {
                Label dash = new Label("_");
                dash.setLayoutX(layoutX + i * spacing);
                dash.setLayoutY(layoutY);
                dash.setStyle("-fx-font-size: 40px; -fx-text-fill: white; -fx-font-family: 'Dialog'; -fx-alignment: center;");
                dash.setVisible(true);
                wordsAP.getChildren().add(dash);
                listaCrtica.add(dash);
            }
        } else {
            System.out.println("AnchorPane za crtice ili template labela nisu pronađeni na sceni.");
        }
    }

    @FXML
    public void ConnectUser(ActionEvent event) {
        this.chatClient = new ChatClient("localhost", 12345, this, nameTF.getText().trim());
        ChatClientManager.setInstance(chatClient);
        this.chatClient.start();

        this.currentClient = chatClient;

        unesiIme.setDisable(true);
    }

    public void updateUserList(String[] users) {
        Platform.runLater(() -> {
            igraciLV.getItems().clear();
            igraciLV.getItems().addAll(users);
        });
    }

    public String getUsername() {
        return this.nameTF.getText().trim();
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
            /*
            if (result.isPresent() && result.get() == acceptButton) {
                chatClient.sendMessage("REQUEST_ACCEPTED " + fromUser);

            } else {
                chatClient.sendMessage("REQUEST_DECLINED " + fromUser);
            }

             */
            if (result.isPresent() && result.get() == acceptButton) {
                ChatClientManager.getInstance().sendMessage("REQUEST_ACCEPTED " + fromUser);
            } else {
                ChatClientManager.getInstance().sendMessage("REQUEST_DECLINED " + fromUser);
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

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    @FXML
    private void handleLetterGuess(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        String guessedLetter = clickedButton.getText();
        ChatClientManager.getInstance().sendMessage("/letter " + guessedLetter);
    }


    public void updateWordDisplay(String guessedWord) {
        for (int i = 0; i < guessedWord.length(); i++) {
            char ch = guessedWord.charAt(i);
            Label label = listaCrtica.get(i);
            label.setText(String.valueOf(ch));
        }
    }

    public void handleServerMessage(String message) {
        if (message.startsWith("correctLetter")) {
            String letter = message.substring("correctLetter ".length());
            updateWordDisplay(letter);
        } else if (message.startsWith("incorrectLetter")) {
            String letter = message.substring("incorrectLetter ".length());
            updateWordDisplay(letter);
        } else if (message.startsWith("end")) {
            String result = message.substring("end ".length());
            if (result.equals("WIN")) {
                //showEndMessage("Congratulations! You won!");
            } else if (result.equals("LOSS")) {
                //showEndMessage("Game over! You lost!");
            }
        }
    }



}
