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
    private Label labelName, labelCategory, labelScore; //za popunjavanje kad se pokrene igra
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
    private static CategoryWords categoryWords;
    private Scene pocetna;
    private Stage primaryStage;
    private ChatClient chatClient;
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

        // Add a shutdown hook to handle client cleanup
        primaryStage.setOnCloseRequest(event -> {
            if (chatClient != null && chatClient.isAlive()) {
                chatClient.shutdown();
            }
            System.exit(0);
        });
    }

    public static void main(String[] args) {
        categoryWords = new CategoryWords(FILE_NAME);
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

        // Pronalaženje elemenata u trenutnoj sceni
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
            }
        } else {
            System.out.println("AnchorPane za crtice ili template labela nisu pronađeni na sceni.");
        }
    }

    @FXML
    public void ConnectUser(ActionEvent event) {
        this.chatClient = new ChatClient("localhost", 12345, this, nameTF.getText().trim());
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

                // Nakon što ste prihvatili zahtjev, trebali biste pričekati odgovor od servera
                // koji će sadržavati informacije o igri
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

    public Stage getPrimaryStage() {
        return primaryStage;
    }
}
