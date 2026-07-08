package com.chenzhen.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.*;

@RestController
public class ClientController {

    @RequestMapping("uploadFile")
    public String uploadFile(@RequestParam("fileName") MultipartFile file)  {
        String fileName = file.getOriginalFilename();
        InputStream input = null;
        DataOutputStream pw = null;
        try {
            input = file.getInputStream();
            byte[] bytes = new byte[1024];
            int len = -1;
            File filepath = new File("./upload/");
            if(!filepath.exists()) {
                filepath.mkdirs();
            }
            File fileNew = new File("./upload/", fileName);
            if(fileNew.exists()) {
                fileNew.delete();
            }
            fileNew.createNewFile();
            pw = new DataOutputStream(new FileOutputStream(fileNew));
            while((len = input.read(bytes)) != -1) {
                pw.write(bytes,0, len);
            }
            pw.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                pw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "success";
    }

}
