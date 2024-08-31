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
    private AnchorPane hangmanAP, wordsAP; //anchor za covjeka - napraviti transparentno, wordsAP za crtice
    @FXML
    private Label crticaLabel; //TODO: ZA KOPIRANJE CRTICA
    @FXML
    private Line lijevaNoga, desnaNoga, lijevaRuka, desnaRuka, tijelo, nos;
    @FXML
    private Circle glava, lijevoOko, desnoOko;
    @FXML
    private QuadCurve usne;
    @FXML
    private Button A, B, C, Č, Ć, D, Dž, Đ, E, F, G, H, I, J, K, L, Lj, M, N, Nj, O, P, R, S, Š, T, U, V, Z, Ž;
    private Scene pocetna;
    private Stage primaryStage;
    private ChatClient chatClient;
    private Parent root;
    private String guessedLetter;
    private String playerName;
    private List<String> letters = new ArrayList<>();
    private static ChatServer chatServer;


    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;
        stage.setTitle("HANGMAN");

        // Load the initial scene
        root = FXMLLoader.load(getClass().getResource("Scene/pocetna_scena.fxml"));
        Scene scene = new Scene(root, 850, 600);
        stage.setScene(scene);

        stage.show();

        // Add a shutdown hook to handle client cleanup
        primaryStage.setOnCloseRequest(event -> {
            if (chatClient != null && chatClient.isAlive()) {
                chatClient.shutdown();
            }
            System.exit(0);
        });
    }

    public static void main(String[] args) {
        chatServer = ChatServer.getInstance();
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

        // Pronalaženje elemenata u trenutnoj sceni
        AnchorPane wordsAP = (AnchorPane) currentScene.lookup("#wordsAP");
        Label crticaLabel = (Label) currentScene.lookup("#crticaLabel");
        Label labelCategory = (Label) currentScene.lookup("#labelCategory");
        labelTurn = (Label) currentScene.lookup("#labelTurn");
        labelName = (Label) currentScene.lookup("#labelName");

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

            double spacing = 40; // Razmak između crtica
            double totalWidth = word.length() * spacing; // Ukupna širina crtica

            double layoutX = (wordsAP.getWidth() - totalWidth) / 2;
            double layoutY = crticaLabel.getLayoutY();

            for (int i = 0; i < word.length(); i++) {
                Label dash;
                if(word.charAt(i) == ' '){
                    dash = new Label(" ");
                }else{
                    dash = new Label("_");
                }
                letters.add(dash.getText());
                dash.setLayoutX(layoutX + i * spacing);
                dash.setLayoutY(layoutY);
                dash.setStyle("-fx-font-size: 40px; -fx-text-fill: white; -fx-font-family: 'Dialog'; -fx-alignment: center;");
                dash.setVisible(true);
                wordsAP.getChildren().add(dash);
            }
        } else {
            System.out.println("AnchorPane za crtice ili template labela nisu pronađeni na sceni.");
        }
    }

    @FXML
    public void ConnectUser(ActionEvent event) {
        this.playerName = nameTF.getText().trim();
        this.chatClient = new ChatClient("localhost", 12345, this, playerName);
        this.chatClient.start(); // Start the ChatClient thread

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

    public void getGuessedLetter(ActionEvent event) {
        Object source = event.getSource();

        if (source instanceof Button) {
            Button button = (Button) source;
            String letter = button.getText();
            this.guessedLetter = letter;
        }


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

    public void setTurnLabel(String message){
        labelTurn.setText(message);
    }

    public void updateDash(String letter, int position){
        letters.set(position, letter);
    }

    public void updateGUIDash() {
        Platform.runLater(() -> {
            // Get the current scene
            Scene currentScene = primaryStage.getScene();
            if (currentScene == null) {
                System.err.println("Error: currentScene is null");
                return;
            }

            // Find the AnchorPane and the labels for dashes
            AnchorPane wordsAP = (AnchorPane) currentScene.lookup("#wordsAP");
            if (wordsAP == null) {
                System.err.println("Error: wordsAP is null");
                return;
            }

            // Clear existing children in wordsAP
            wordsAP.getChildren().clear();

            // Calculate spacing and layout
            double spacing = 40;
            double totalWidth = letters.size() * spacing;
            double layoutX = (wordsAP.getWidth() - totalWidth) / 2;
            double layoutY = 40; // Adjust based on your layout

            // Create and add new labels based on the updated letters list
            for (int i = 0; i < letters.size(); i++) {
                Label dash = new Label(letters.get(i));
                dash.setLayoutX(layoutX + i * spacing);
                dash.setLayoutY(layoutY);
                dash.setStyle("-fx-font-size: 40px; -fx-text-fill: white; -fx-font-family: 'Dialog'; -fx-alignment: center;");
                dash.setVisible(true);
                wordsAP.getChildren().add(dash);
            }
        });
    }


    public void showNotYourTurnAlert() {
        // Ensure GUI updates are done on the JavaFX Application Thread
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING, "It's not your turn!", ButtonType.OK);
            alert.setTitle("Turn Notification");
            alert.setHeaderText(null); // Optionally set header text
            alert.showAndWait();
        });
    }

    public void updateChat(String chatMessage) {
        Platform.runLater(() -> {
            chatLV.getItems().add(chatMessage);
        });
    }


}
