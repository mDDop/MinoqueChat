package pl.hubertgawrys.simplychat.models;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@EnableWebSocket
@Component
public class ChatSocket extends TextWebSocketHandler implements WebSocketConfigurer {

    private List<UserModel> sessionList = new ArrayList<>();


    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
        webSocketHandlerRegistry.addHandler(this, "/chat").setAllowedOrigins("*");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        UserModel userModel = findUserBySessionId(session.getId());
        String messageString = message.getPayload();
        if (!userModel.isUserBanned()) {
            if (userModel.getUsername()!=null && userModel.isUserAdmin()) {
                if (messageString.startsWith("/kick")){
                    String[] userBaned = messageString.split(" ");
                    if(userBaned.length < 2){
                        userModel.sendMessage("server:You forgot about username to kick!");
                    } else if (!existUser(userBaned[1])){
                        userModel.sendMessage("server:There are no user: " + userBaned[1]);
                    } else {
                        sessionList.remove(sessionList.indexOf(findUserByUsername(userBaned[1])));
                    }
                    return;
                }
                if (messageString.startsWith("/ban")){
                    String[] userBaned = messageString.split(" ");
                    if(userBaned.length < 2){
                        userModel.sendMessage("server:You forgot about username to ban!");
                    } else if (!existUser(userBaned[1])){
                        userModel.sendMessage("server:There are no user: " + userBaned[1]);
                    } else {
                        System.out.println(userBaned[1]);
                        System.out.println(sessionList.toString());
                        sessionList.get(sessionList.indexOf(findUserByUsername(userBaned[1])))
                                .setBanTime(LocalTime.now().plusMinutes(5));
                    }
                    return;
                }
            }
                if (messageString.equals("") || messageString == null) {
                    userModel.sendMessage("server: Message can't be empty!");
                } else {
                    if (userModel.getUsername() == null) {
                        if (existUser(messageString)) {
                            userModel.sendMessage("server: This username exist already!");
                            return;
                        }
                        userModel.setUsername(messageString);
                        userModel.sendMessage("server:Your username has been set!");
                        userModel.getLocalTimes().remove(0);
                        return;
                    }
                    if (userModel.getUsername() == null) {
                        sendMessageToAll("log:" + userModel.getUsername() + ": " + message.getPayload());

                    }
                }

                sendMessageToAll("log:" + userModel.getUsername() + ": " + LocalTime.now() + " : " + message.getPayload());
            }

    }




    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        UserModel userModel = new UserModel(session);
        sessionList.add(new UserModel(session));
        userModel.sendMessage("server:Welcome on Chat!");
        userModel.sendMessage("server:Your first message will become your username!");
        sendMessageToAll("connected:" + sessionList.size());
    }

    private void sendMessageToAll(String message) throws IOException {
        for (UserModel userModel : sessionList) {
            userModel.sendMessage(message);
        }
    }

    private UserModel findUserBySessionId(String sessionId) {
        return sessionList.stream()
                .filter(s -> s.getSession().getId().equals(sessionId))
                .findAny()
                .orElseThrow(IllegalStateException::new);
    }

    private UserModel findUserByUsername(String username) {
        return sessionList.stream()
                .filter(s -> s.getUsername().equals(username))
                .findAny()
                .orElseThrow(IllegalStateException::new);
    }

    private boolean existUser(String username) {
        return sessionList.stream()
                .anyMatch(s -> s.getUsername() != null && s.getUsername().equals(username));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessionList.remove(findUserBySessionId(session.getId()));
        sendMessageToAll("connected:" + sessionList.size());
    }
}
