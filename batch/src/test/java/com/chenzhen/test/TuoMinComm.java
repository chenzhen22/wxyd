package com.chenzhen.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class TuoMinComm {

    public static List<String> readFile(String serviceName) throws Exception {
        File file = new File("D:\\GIT\\TBP\\"+serviceName+"\\designSource\\common\\dataDict\\dataDict.xml");
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String msg = null;
        List<String> mlist = new ArrayList<>();
        if(file.exists()) {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            while ((msg = br.readLine()) != null) {
                mlist.add(msg);
            }
        }
        file = new File("D:\\GIT\\TBP\\"+serviceName+"\\designSource\\common\\dataDict\\dataDict_newCore.xml");
        if(file.exists()) {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            while ((msg = br.readLine()) != null) {
                mlist.add(msg);
            }
        }

        return mlist;
    }

    public static void print(List<TuominField> list) {
        String ss = "";
        for (TuominField tuominField : list) {
            System.out.println(tuominField.toString());
            ss = ss + tuominField.getName() + ",";
        }
        System.out.println(ss);
    }
}
