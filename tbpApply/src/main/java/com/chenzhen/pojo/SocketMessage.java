package com.chenzhen.pojo;

import lombok.Data;

@Data
public class SocketMessage {
     private String type;
     private String url;
     private String message;
     private String recive;
     private String status;
     private String date;
     private String clientIp;
     private String serverIp;
     private String handleIp;

     public SocketMessage() {
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

     public SocketMessage(String type, String url, String message, String recive, String status, String date, String clientIp, String serverIp) {
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
