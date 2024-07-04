import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class GlavnaScenaController {
    @FXML
    private Label labelCategory;

    private CategoryWords categoryWords;

    public void initialize(CategoryWords categoryWords) {
        this.categoryWords = categoryWords;
    }

    @FXML
    public void startGame() {
        if (categoryWords == null) {
            System.out.println("CategoryWords instance is not initialized.");
            return;
        }

        String[] challenge = categoryWords.loadChallange();
        String category = challenge[0];
        String word = challenge[1];

        labelCategory.setText("Category: " + category);
        // Postavi riječ za igru (možete dodatno implementirati logiku za postavljanje riječi u GUI)
    }
}
