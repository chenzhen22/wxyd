package com.chenzhen.test;

import com.chenzhen.pojo.Number;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ThreadTest {

    //psvm
    public static void main(String[] args) {
        List<Number> list = new ArrayList<Number>();
        new Thread (() -> {

            for(int i=0; i<100; i++) {
                Number number = new Number();
                Random random = new Random();
                int num1 = random.nextInt(33);
                number.setNum1(num1);

                int num2 = random.nextInt(33);
                number.setNum2(num2);

                int num3 = random.nextInt(33);
                number.setNum3(num3);

                int num4 = random.nextInt(33);
                number.setNum4(num4);

                int num5 = random.nextInt(33);
                number.setNum5(num5);

                int num6 = random.nextInt(16);
                number.setNum6(num6);

                System.out.println(number);
            }


        }).start();

    }


}
