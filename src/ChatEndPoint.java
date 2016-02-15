

import com.sun.deploy.net.HttpRequest;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by lv on 2016/1/28.
 */
@ServerEndpoint(value = "/websocket/chat")
public class ChatEndPoint {
    private static final String GUEST_PREFIX = "guest";
    private static final AtomicInteger connectionIds = new AtomicInteger(0);
    private static final Set<ChatEndPoint> clientSet = new CopyOnWriteArraySet<>();
    private final String nickName;
    private Session session;

    public ChatEndPoint() {
        nickName = GUEST_PREFIX + connectionIds.getAndIncrement();
    }

    @OnOpen
    public void start(Session session) {
        this.session = session;
        clientSet.add(this);
        String message = String.format("[%s %s]", nickName, "has joined in...");
        broadcast(message);
    }

    @OnClose
    public void end() {
        clientSet.remove(this);
        String message = String.format("[%s %s]", nickName, "has left...");
        broadcast(message);
    }

    @OnMessage
    public void incoming(String message) {
        String filteredMessage = String.format("%s:%s", nickName, filter(message));
        broadcast(filteredMessage);
    }

    @OnError
    public void onError(Throwable t) throws Throwable{
        System.out.println("WebSocket server error " + t);
    }


    private void broadcast(String msg) {
        for (ChatEndPoint client : clientSet) {
            try {
                synchronized (client) {
                    client.session.getBasicRemote().sendText(msg);
                }
            } catch (IOException e) {
                System.out.println("Error occurred when send message to client " + client);
                try {
                    client.session.close();
                } catch (IOException e1) {

                }
                String message = String.format("[%s %s]", client.nickName, "has disconnected...");
                broadcast(message);
            }
        }
    }

    private static String filter(String message) {
        if (message == null)
            return null;
        char content[] = new char[message.length()];
        message.getChars(0, message.length(), content, 0);
        StringBuilder result = new StringBuilder(content.length + 50);
        for (int i = 0; i < content.length; i++) {
            switch (content[i]) {
                case '<' :
                    result.append("&lt;");
                    break;
                case '>' :
                    result.append("&gt;");
                    break;
                case '&' :
                    result.append("&amp;");
                    break;
                case '"' :
                    result.append("&quot;");
                    break;
                default:
                    result.append(content[i]);
            }
        }
        return result.toString();
    }

}
