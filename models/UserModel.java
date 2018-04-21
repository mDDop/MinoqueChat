package pl.hubertgawrys.simplychat.models;

import lombok.Data;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class UserModel {
    private String username;
    private WebSocketSession session;
    private int counter;
    private List<LocalTime> localTimes = new ArrayList<>();
    private LocalTime banTime = LocalTime.now().minusHours(1);

    public UserModel(WebSocketSession session) {
        this.session = session;
    }

    public void sendMessage(String text) throws IOException {
        session.sendMessage(new TextMessage(text));
    }

    public boolean isUserBanned() throws IOException {
        LocalTime localNowTime = LocalTime.now();
        if (banTime.isAfter(localNowTime)) {
            sendMessage("server:Be patient, you are banned till: " + banTime);
            return true;
        } else {
            if (localTimes.size() < 10) {
                localTimes.add(LocalTime.now());
                System.out.println(localTimes);
                return false;
            } else {
                if (localTimes.get(0).plusMinutes(1).isAfter(localNowTime)) {
                    banTime = (localNowTime.plusMinutes(1));
                    sendMessage("server:You have been banned for one minute till:" + banTime);
                    System.out.println(banTime);
                    return true;
                }
            }
            localTimes.remove(0);
            localTimes.add(LocalTime.now());
            System.out.println(localTimes);
        }
        return false;
    }

    public boolean isUserAdmin(){
        if (username.equals("admin123")){
            return true;
        }
        return false;
    }

    public void kickBanUserByAdmin(){

    }
}

