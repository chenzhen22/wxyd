package com.cyz.pojo;

import lombok.Data;

@Data
public class Message {

     private String type;
     private String url;
     private String message;
     private String recive;
     private String status;
     private String date;
     private String clientIp;
     private String serverIp;
     private String handleIp;

     public Message(String type, String url, String message, String recive, String status, String date, String clientIp, String serverIp, String handleIp) {
          this.type = type;
          this.url = url;
          this.message = message;
          this.recive = recive;
          this.status = status;
          this.date = date;
          this.clientIp = clientIp;
          this.serverIp = serverIp;
          this.handleIp = handleIp;
     }
}
