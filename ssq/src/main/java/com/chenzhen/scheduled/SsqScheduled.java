package com.chenzhen.scheduled;

import com.chenzhen.service.SsqService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Random;

@Component
@Slf4j
public class SsqScheduled {

    @Resource
    SsqService ssqService;

    @Scheduled(fixedRate = 10000)
    private void createSsq() {
        for(int i=0; i<4000; i++) {
            Random random = new Random();
            int num1 = 0;
            int num2 = 0;
            int num3 = 0;
            int num4 = 0;
            int num5 = 0;
            int num6 = 0;
            int num7 = 0;
            do {
                num1 = random.nextInt(33);
                if(num1 != 0) {
                    break;
                }
            } while (true);

            do {
                num2 = random.nextInt(33);
                if(num2 != 0 && num2 != num1) {
                    break;
                }
            } while (true);

            do {
                num3 = random.nextInt(33);
                if(num3 != 0 && num3 != num2 && num3 != num1) {
                    break;
                }
            } while (true);

            do {
                num4 = random.nextInt(33);
                if(num4 != 0 && num4 != num3 && num4 != num2 && num4 != num1) {
                    break;
                }
            } while (true);

            do {
                num5 = random.nextInt(33);
                if(num5 != 0 && num5 != num4 && num5 != num3 && num5 != num2 && num5 != num1) {
                    break;
                }
            } while (true);

            do {
                num6 = random.nextInt(33);
                if(num6 != 0 && num6 != num5 && num6 != num4 && num6 != num3 && num6 != num2 && num6 != num1) {
                    break;
                }
            } while (true);

            do {
                num7 = random.nextInt(16);
                if(num7 != 0) {
                    break;
                }
            } while (true);
            int[] arr = {num1, num2, num3, num4, num5, num6};
            Arrays.sort(arr);
            log.info("number:{}", arr);
            String ss1 = (arr[0]+"").length()==1?"0"+(arr[0]+""):(arr[0]+"");
            String ss2 = (arr[1]+"").length()==1?"0"+(arr[1]+""):(arr[1]+"");
            String ss3 = (arr[2]+"").length()==1?"0"+(arr[2]+""):(arr[2]+"");
            String ss4 = (arr[3]+"").length()==1?"0"+(arr[3]+""):(arr[3]+"");
            String ss5 = (arr[4]+"").length()==1?"0"+(arr[4]+""):(arr[4]+"");
            String ss6 = (arr[5]+"").length()==1?"0"+(arr[5]+""):(arr[5]+"");
            String ss7 = (num7+"").length()==1?"0"+(num7+""):(num7+"");
            String num = ss1+" "+ss2+" "+ss3+" "+ss4+" "+ss5+" "+ss6+" "+ss7;
            ssqService.updateNum(num);

        }
    }

}
