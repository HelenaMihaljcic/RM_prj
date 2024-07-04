import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class CategoryWords {
    private HashMap<String, String[]> wordList;
    private ArrayList<String> categories;

    public CategoryWords(String fileName) {
        wordList = new HashMap<>();
        categories = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                String category = parts[0];
                String[] words = new String[parts.length - 1];
                System.arraycopy(parts, 1, words, 0, parts.length - 1);

                addCategory(category, words);
            }
        } catch (IOException e) {
            System.out.println("Greska prilikom ucitavanja datoteke");
            e.printStackTrace();
        }
    }

    private void addCategory(String category, String[] words) {
        wordList.put(category, words);
        categories.add(category);
    }

    public HashMap<String, String[]> getWordList() {
        return wordList;
    }

    public ArrayList<String> getCategories() {
        return categories;
    }

    public String[] loadChallange(){
        Random rand = new Random();

        String category = categories.get(rand.nextInt(categories.size()));
        String[] categoryValues = wordList.get(category);
        String word = categoryValues[rand.nextInt(categoryValues.length)];

        // [0] -> category, [1] -> word
        return new String[]{category.toUpperCase(), word.toUpperCase()};
    }

    @Override
    public String toString() {
        return "CategoryWords{" +
                "wordList=" + wordList +
                ", categories=" + categories +
                '}';
    }
}
