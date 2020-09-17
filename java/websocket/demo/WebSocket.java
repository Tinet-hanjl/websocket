package websocket.demo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ServerEndpoint(value = "/connectWebSocket/{userId}")
public class WebSocket {

    private Logger logger= LoggerFactory.getLogger(getClass());
    /**
     * 在线人数
     */
    public static int onlineNumber = 0;
    /**
     * 以用户的姓名为key，WebSocket为对象保存起来
     */
    private static Map<String, WebSocket> clients = new ConcurrentHashMap<String, WebSocket>();
    /**
     * 会话
     */
    private Session session;
    /**
     * 用户名称
     */
    private String userId;

    /**
     * 建立连接
     * @param userId
     * @param session
     */
    @OnOpen
    public void onOpen(@PathParam("userId") String userId, Session session){
        onlineNumber++;
        logger.info("现在连接的客户id:"+session.getId()+"用户名："+userId);
        this.userId=userId;
        this.session=session;
        try {
            //messageType 1代表上线 2代表下线 3代表在线名单  4代表普通消息
            Map<String,Object> map1=new HashMap<>();
            map1.put("messageType",1);
            map1.put("userId",userId);
            sendMessageAll(JSON.toJSONString(map1),userId);
            clients.put(userId,this);

            logger.info("有连接接入！当前在线人数"+clients.size());
            //给自己发一条消息：告诉自己现在都有谁在线
            Map<String,Object> map2 = new HashMap<>();
            map2.put("messageType",3);
            //移除掉自己
            Set<String> set = clients.keySet();
            map2.put("onlineUsers",set);
            sendMessageTo(JSON.toJSONString(map2),userId);
        } catch (IOException e) {
            logger.info(userId+"上线的时候通知所有人发生了错误");
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        logger.info("服务端发生了错误"+error.getMessage());
    }

    /**
     * 连接关闭
     */
    @OnClose
    public void onClose(){
        onlineNumber--;
        clients.remove(userId);
        try {
            Map<String, Object> map1=new HashMap<>();
            map1.put("messageType",2);
            map1.put("onlineUsers",clients.keySet());
            map1.put("userId",userId);
            sendMessageAll(JSON.toJSONString(map1),userId);
        } catch (IOException e) {
           logger.info(userId+"下线时发生错误");
        }
        logger.info("有连接关闭！当前在线人数"+clients.size());
    }

    @OnMessage
    public void onMessage(String message, Session session){
        try {
            logger.info("来自客户端的消息："+message+"客户id为："+userId);
            JSONObject jsonObject=JSON.parseObject(message);
            String textMessage = jsonObject.getString("message");
            String fromUserId = jsonObject.getString("userId");
            String toUserId = jsonObject.getString("to");

            Map<String, Object> map1=new HashMap<>();
            map1.put("messageType",4);
            map1.put("textMessage", textMessage);
            map1.put("fromUserId",fromUserId);
            if (toUserId.equals("All")){
                map1.put("toUserId", "所有人");
                sendMessageAll(JSON.toJSONString(map1),fromUserId);
            }else {
                map1.put("toUserId",toUserId);
                System.out.println("开始推送消息"+toUserId);
                sendMessageTo(JSON.toJSONString(map1),fromUserId);
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.info("发生了错误");
        }
    }

    public void sendMessageTo(String message, String toUserId) {
        for (WebSocket item : clients.values()) {
            if (item.userId.equals(toUserId)){
                item.session.getAsyncRemote().sendText(message);

                break;
            }
        }
    }

    public void sendMessageAll(String message, String fromUserId)throws IOException{
        for (WebSocket item:clients.values()) {
            item.session.getAsyncRemote().sendText(message);
        }
    }
    public static synchronized int getOnlineCount(){
        return onlineNumber;
    }
}
