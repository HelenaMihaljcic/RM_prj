import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

final class ClientWriteThread extends Thread {
    private final String username;
    private PrintWriter toServer;
    private Main main;

    ClientWriteThread(String username, Socket socket, Main main) {
        this.username = username;
        this.main = main;
        try {
            this.toServer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException ex) {
            System.out.println("Error getting output stream: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    public void run() {
        // First send username, server expects that info
        this.toServer.println(this.username);

        // Then send input to server line by line, until `bye`
        try (Scanner sc = new Scanner(System.in)) {
            String text;
            do {
                System.out.printf("\r[%s]: ", this.username);
                text = sc.nextLine();
                this.toServer.println(text);
            } while (!text.equals("bye"));
        }
    }
}
