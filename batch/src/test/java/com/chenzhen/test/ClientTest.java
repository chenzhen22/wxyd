package com.chenzhen.test;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class ClientTest extends ApplicationContextRunner {

    @Test
    public void test1(){
//        String fileName = "C:\\Users\\chenzhen.GHBANK\\Desktop\\query-impala-5924.xlsx";
//        EasyExcel.read(fileName, OSBData.class, new OSBDataListener()).sheet().doRead();

        }

    @Test
    public void test2() {
        new Thread(() ->{
            System.out.println("hello");
        });

    }

}
