package com.chenzhen.test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class TransTest {
    public static List<String> list = new ArrayList<String>();

    static {
        list.add("CPR02056");
        list.add("CPR030049");
        list.add("CPR030049");
        list.add("CPR030056");
        list.add("CPR030058");
        list.add("CPR030093");
        list.add("CPR040101");
        list.add("CPR04065");
        list.add("CPR04065");
        list.add("CPR05009");
        list.add("CPR05009");
        list.add("CPR05011");
        list.add("CPR05013");
        list.add("CPR05054");
        list.add("CPR05093");
        list.add("CPR05096");
        list.add("CPR05098");
        list.add("CPR05099");
        list.add("CPR05099");
        list.add("CPR05099");
        list.add("CPR06060");
        list.add("CPR06060");
        list.add("CPR06087");
        list.add("CPR06087");
        list.add("CPR06087");
        list.add("CPR06090");
        list.add("CPR06092");
        list.add("CPR06093");
        list.add("CPR06095");
        list.add("CPR06096");
        list.add("CPR06098");
        list.add("CPR06147");
        list.add("CPR11016");
        list.add("CPR11019");
        list.add("CPR11024");
        list.add("CPR11038");
        list.add("CPR11040");
        list.add("CPR11044");
        list.add("CPR11101");
        list.add("CPR12005");
        list.add("CPR12018");
        list.add("CPR12019");
        list.add("CPR21004");
        list.add("CPR21007");
        list.add("CPR21012");
        list.add("CPR21014");
        list.add("CPR22052");
        list.add("CPR22075");
        list.add("CPR23035");
        list.add("CPR23036");
        list.add("CPR23037");
        list.add("CPR23042");
        list.add("CPR24037");
        list.add("CPR24112");
        list.add("CPR24165");
        list.add("CPR24185");
        list.add("CPR24187");
        list.add("CPR24189");
        list.add("CPR400034");
        list.add("CPR400037");
        list.add("CPR400052");
        list.add("CPR400056");
        list.add("CPR90001");
        list.add("CPR90631");
    }


    public static void main(String[] args) {
        BufferedReader br = null;



        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream("C:\\Users\\chenzhen.GHBANK\\Desktop\\tran.txt")));

            String msg = null;
            while ((msg = br.readLine()) != null) {
                if (msg.startsWith("<entry key")) {
                    String[] msg1 = msg.split("\" value");
                    String[] msg2 = msg1[0].split("<entry key=\"");
                    if(!list.contains(msg2[1])) {
                        System.out.println(msg2[1]);
                    }

                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
