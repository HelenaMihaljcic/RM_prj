import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server {

    private static Map<String, List<String>> mapaRijeci;

    public static void main(String[] args) {
        Server.ucitajKategorije();

        System.out.println(mapaRijeci + "");
    }

    public static void ucitajKategorije(){
        mapaRijeci = new HashMap<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get("Kategorije"))) {
            for (Path path : directoryStream) {
                if (Files.isRegularFile(path)) {
                    String leagueName = path.getFileName().toString().replace(".txt", "");
                    List<String> contentLines = Files.readAllLines(path);
                    mapaRijeci.put(leagueName, contentLines);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
