package com.chenzhen.test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockTest {
    private static Lock lock = new ReentrantLock();


    public static void main(String[] args) throws InterruptedException {
        new Thread(()->{
            try {
                aoo();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(()->{
            try {
                boo();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(()->{
            coo();
        }).start();

    }

    public static void aoo() throws InterruptedException {
        if(lock.tryLock(2, TimeUnit.SECONDS)) {
            try {
                System.out.println(1111);
                TimeUnit.SECONDS.sleep(3);
                System.out.println(111);
            } finally {
                lock.unlock();
            }
        }
    }

    public static void boo() throws InterruptedException {
        lock.lock();
        System.out.println(2222);
        TimeUnit.SECONDS.sleep(3);
        System.out.println(222);
        lock.unlock();
    }

    public static void coo() {
        lock.lock();
        System.out.println(333);
        lock.unlock();
    }
}
