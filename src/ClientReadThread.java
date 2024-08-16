import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
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

                System.err.println(response + "");

                if (response.startsWith("UPDATE_USERS")) {
                    String users = response.substring(13);
                    String[] userArray = users.split(",");
                    Platform.runLater(() -> {
                        main.updateUserList(userArray);
                    });
                } else if (response.startsWith("/response ")) {
                    String fromUser = response.substring(10);
                    main.receivedRequest(fromUser);
                } else if (response.startsWith("REQUEST_DECLINED")) {
                    main.requestDeclined();
                } else if (response.contains("REQUEST_ACCEPTED ")) {
                    System.out.println("Radi kod mene sto sam poslala zahtjev");
                    Platform.runLater(() -> {
                        try {
                            main.switchToGameScene();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                } else {
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
