package com.edu.pollingquery;

import com.edu.pollingquery.context.BoundResultRequestContextHolder;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.edu.pollingquery.processor.ResultProcessorType;
import com.edu.pollingquery.server.AbstractQueryServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;


@EnableScheduling
@EnableAsync
@EntityScan(basePackages = {"com.edu.pollingcache"})
@ImportAutoConfiguration({GsonAutoConfiguration.class, PollingQueryConfiguration.class})
@SpringBootApplication()
public class SpringBootApplicationTest implements CommandLineRunner {

    @Autowired
    private AbstractQueryServer<String> ganttQueryServer;

    @Autowired
    private AbstractQueryServer<List<String>> queryServer;

    public static void main(String[] args) {
        SpringApplication.run(SpringBootApplicationTest.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        int num = 3;
        AsyncTaskExecutor threadPool = createThreadPool(num);
        for(int i =0 ; i< num; i++) {
            if(i%2 == 0) {
                BoundResultRequestContextHolder<List<String>> bindingHolder =
                        new BoundResultRequestContextHolder<List<String>>("FLIGHT_LIST", (o -> {
                            try {
                                Thread.sleep(2000L);
                                System.out.println("finish query flight list!");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            return Lists.newArrayList("1", "2");
                        }), null, null, null);
                bindingHolder.setSupportProcessorTypes(Sets.newHashSet(ResultProcessorType.FILTER,ResultProcessorType.USER_INFO_HANDLER));
                System.out.println("FLIGHT_TIME:"+ bindingHolder.getReqStartTime().getTime() +";h" + bindingHolder.getReqStartTime().hashCode());
                queryServer.registerQueryRequest(bindingHolder);
                submit(threadPool, bindingHolder);
            }else{
                BoundResultRequestContextHolder<String> bindingHolder =
                        new BoundResultRequestContextHolder<String>("GANT_INFO", (o -> {
                            try {
                                Thread.sleep(2000L);
                                System.out.println("finish query gantt info!");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            return "GANT_RESULT";
                        }), null, null, null);
                System.out.println("GANTT_TIME:"+ bindingHolder.getReqStartTime().getTime() +";h" + bindingHolder.getReqStartTime().hashCode());
                ganttQueryServer.registerQueryRequest(bindingHolder);
                submit(threadPool, bindingHolder);
            }
        }
    }

    private void submit(AsyncTaskExecutor threadPool, BoundResultRequestContextHolder<?> bindingHolder){
        threadPool.submit(() -> {
            while(!bindingHolder.isSetOrExpired()){
                try {
                    Thread.sleep(200L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("RT:"+ bindingHolder.getReqStartTime().getTime()
                            + ";RS:" + bindingHolder.getResult()+
                            ";OBJ:" + bindingHolder.hashCode());

        });
    }

    private AsyncTaskExecutor createThreadPool(int num){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //线程名称前缀
        executor.setThreadNamePrefix(num + "-executor-");
        //线程创建工厂 匿名内部类实现
        executor.setThreadFactory(new CustomizableThreadFactory(executor.getThreadNamePrefix()) {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread t = createThread(runnable);
                //设置子线程异常日志记录
                t.setUncaughtExceptionHandler((thread, e) -> System.out.println(thread + " throws exception: " + e));
                return t;
            }
        });
        //定义核心线程数
        executor.setCorePoolSize(num);
        executor.setMaxPoolSize(num);
        executor.setQueueCapacity(1);
        executor.setKeepAliveSeconds(0);
        //设置拒绝处理策略为DiscardOldestPolicy
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        executor.initialize();
        return executor;
    }
}
