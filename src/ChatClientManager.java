public class ChatClientManager {
    private static ChatClient instance;

    private ChatClientManager() {}

    public static ChatClient getInstance() {
        return instance;
    }

    public static void setInstance(ChatClient chatClient) {
        instance = chatClient;
    }
}
