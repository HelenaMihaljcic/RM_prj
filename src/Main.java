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
import java.util.Optional;

public class Main extends Application {
    private static final String FILE_NAME = "data.txt";
    public Button unesiIme;

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
    private UserThread userThread;
    private Parent root;


    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;
        stage.setTitle("HANGMAN");

        // Load the initial scene
         root = FXMLLoader.load(getClass().getResource("Scene/pocetna_scena.fxml"));
        pocetna = new Scene(root, 850, 600);
        stage.setScene(pocetna);

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

    public void setGlavnaScena(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("Scene/glavna_scena.fxml"));
        primaryStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        pocetna = new Scene(root);
        primaryStage.setScene(pocetna);
        primaryStage.show();

        startGame();
    }

    public void switchToGameScene() throws IOException {
        if (primaryStage == null) {
            System.err.println("Error: primaryStage is null");
            return;
        }
        try {
            // Load the new scene
             root = FXMLLoader.load(getClass().getResource("Scene/glavna_scena.fxml"));
            Scene gameScene = new Scene(root);
            primaryStage.setScene(gameScene);

            // Update the primary stage and show it
            primaryStage.show();

            // Start the game logic
            startGame();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startGame() {
        String[] challenge = categoryWords.loadChallange();
        String category = challenge[0];
        String word = challenge[1];

        labelCategory = (Label) pocetna.lookup("#labelCategory");
        if (labelCategory != null) {
            labelCategory.setText(category);
        } else {
            System.out.println("Labela za kategoriju nije pronađena na sceni.");
        }

        System.out.println("Kategorija: " + category + " rijec: " + word);

        wordsAP = (AnchorPane) pocetna.lookup("#wordsAP");
        crticaLabel = (Label) pocetna.lookup("#crticaLabel");

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
    public void Connect(ActionEvent event) {
        this.chatClient = new ChatClient("localhost", 12345, this, nameTF.getText().trim());
        this.chatClient.start(); // Start the ChatClient thread
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
                try {
                    this.switchToGameScene();  // Switch to the game scene
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                chatClient.sendMessage("REQUEST_DECLINED " + fromUser);
            }
        });
    }

    @FXML
    public void handlePlayGame(ActionEvent event) {
        String selectedUser = igraciLV.getSelectionModel().getSelectedItem();
        System.out.println(selectedUser + " izabran");
        if (selectedUser != null) {
            chatClient.sendMessage("/request " + selectedUser);
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
}
