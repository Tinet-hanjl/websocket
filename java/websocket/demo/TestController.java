package websocket.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @Autowired
    private WebSocket webSocket;
    @RequestMapping(value = "/sendTo/{userId}/{mes}")
    public String sendTo(@PathVariable("userId")String userId, @PathVariable("mes") String mes){
        webSocket.sendMessageTo(mes,userId);
        return "推送成功";
    }
}
