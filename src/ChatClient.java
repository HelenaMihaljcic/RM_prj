import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public final class ChatClient extends Thread {
    private final String hostname;
    private final int port;
    private String name;
    private Main main;
    private Socket socket;

    public ChatClient(String hostname, int port, Main main) {
        this.hostname = hostname;
        this.port = port;
        this.main = main;
    }

    @Override
    public void run() {
        try {
            this.setName();
            this.socket = new Socket(this.hostname, this.port);
            System.out.println("Connected to the chat server @ " + this.hostname);

            // Dispatch threads
            ClientReadThread rt = new ClientReadThread(this.name, socket);
            rt.start();
            ClientWriteThread wt = new ClientWriteThread(this.name, socket);
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
}
