import javafx.application.Platform;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public final class ChatClient extends Thread {
    private final String hostname;
    private final int port;
    private String name;
    private Main main;
    private Socket socket;
    private PrintWriter zaServer;
    private InputStream streamForMain;
    private OutputStream outForMain;

    public ChatClient(String hostname, int port, Main main, String name) {
        this.hostname = hostname;
        this.port = port;
        this.main = main;
        this.name = name;
    }

    @Override
    public void run() {
        try {
            this.socket = new Socket(this.hostname, this.port);
            zaServer = new PrintWriter(socket.getOutputStream(), true);
            zaServer.println(name);
            streamForMain = socket.getInputStream();
            outForMain = socket.getOutputStream();

            System.out.println("Connected to the chat server @ " + this.hostname);

            // Dispatch threads
            ClientReadThread rt = new ClientReadThread(this.name, socket, main, main.getPrimaryStage());
            rt.start();
            ClientWriteThread wt = new ClientWriteThread(this.name, socket, main);
            wt.start();

            // Wait for threads, so we can close the socket (try-with-resources)
            rt.join();
            wt.join();

        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            closeSocket();
        }
    }

    private void setName() throws IOException {
        this.name = main.getUsername();
    }

    public void sendMessage(String message) {
        if (zaServer != null) {
            zaServer.println(message);
        } else {
            System.err.println("Cannot send message, PrintWriter is not initialized.");
        }
    }

    public void shutdown() {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.interrupt();
    }

    private void closeSocket() {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public InputStream getStreamForMain() {
        return streamForMain;
    }

    public OutputStream getOutForMain() {
        return outForMain;
    }
}
