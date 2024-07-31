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

public class Main extends Application {
    private static final String FILE_NAME = "data.txt";

    @FXML
    private ListView<String> igraciLV; // za prikazivanje igraca na pocetku
    @FXML
    private Button pokreniDugme, chatButton;
    @FXML
    private ListView<String> chatLV;  // za prikazivanje chata
    @FXML
    private TextField chatTF, nameTF;  // za unos poruke u chat
    @FXML
    private Label labelName, labelCategory = new Label(), labelScore; //za popunjavanje kad se pokrene igra
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

    @Override
    public void start(Stage stage) throws Exception {
        System.out.println("Created ");
        this.primaryStage = stage;
        stage.setTitle("HANGMAN");

        Parent root = FXMLLoader.load(getClass().getResource("Scene/pocetna_scena.fxml"));
        Scene scene = new Scene(root, 850, 600);
        stage.setScene(scene);

        stage.show();

        // Add a shutdown hook to handle client cleanup
        stage.setOnCloseRequest(event -> {
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
        this.chatClient = new ChatClient("localhost", 12345, this);
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
}
