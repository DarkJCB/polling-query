package com.edu.pollingquery.priorityqueue;

import com.google.common.collect.Queues;
import com.edu.pollingquery.context.BoundResultRequestContextHolder;

import java.sql.SQLOutput;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;

/**
 * 测试排序
 *
 * @author jcb
 * @since 2021/2/22
 */
public class SortHolder {

    public static void main(String[] args){
        Queue<BoundResultRequestContextHolder<String>> queue = Queues.synchronizedQueue(
                new PriorityQueue<BoundResultRequestContextHolder<String>>(11,
                        Comparator.comparing(BoundResultRequestContextHolder::getReqStartTime)
                ));
        Thread thread1 = new Thread(() -> {
            for(int i=0;i <10;i ++){
                Date date = new Date(new Date().getTime() + i);
                BoundResultRequestContextHolder<String> h = new BoundResultRequestContextHolder<String>(
                        "viewCode", date, "", "" ,null ,null , null, null
                );
                queue.offer(h);
            }
        });

        Thread thread2 = new Thread(() -> {
            for(int i=0;i <10;i ++){
                Date date = new Date(new Date().getTime() + i);
                BoundResultRequestContextHolder<String> h = new BoundResultRequestContextHolder<String>(
                        "viewCode", date, "", "" ,null ,null , null, null
                );
                queue.offer(h);
            }
        });

        Thread thread3 = new Thread(() -> {
            for(int i=0;i <10;i ++){
                Date date = new Date(new Date().getTime() + i);
                BoundResultRequestContextHolder<String> h = new BoundResultRequestContextHolder<String>(
                        "viewCode", date, "", "" ,null ,null , null, null
                );
                queue.offer(h);
            }
        });

        Thread thread4 = new Thread(() -> {
            for(int i=0;i <10;i ++){
                Date date = new Date(new Date().getTime() + i);
                BoundResultRequestContextHolder<String> h = new BoundResultRequestContextHolder<String>(
                        "viewCode", date, "", "" ,null ,null , null, null
                );
                queue.offer(h);
            }
        });

        Thread thread5 = new Thread(() -> {
            for(int i=0;i <10;i ++){
                Date date = new Date(new Date().getTime() + i);
                BoundResultRequestContextHolder<String> h = new BoundResultRequestContextHolder<String>(
                        "viewCode", date, "", "" ,null ,null , null, null
                );
                queue.offer(h);
            }
        });

        thread1.start();thread2.start();thread3.start();thread4.start();thread5.start();
        try{
            Thread.sleep(1000L);
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
        int processCnt = 0;
        int size = queue.size();
        while(!queue.isEmpty() && processCnt <= size) {
            ++processCnt;
            System.out.println(queue.poll().getReqStartTime().getTime());
        }
    }
}
