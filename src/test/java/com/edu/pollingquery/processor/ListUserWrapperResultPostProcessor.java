package com.edu.pollingquery.processor;

import com.edu.pollingquery.context.BoundResultRequestContextHolder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 模拟用户信息处理器
 *
 * @author jcb
 * @since 2021/1/28
 */
@Order(2)
@Component
public class ListUserWrapperResultPostProcessor implements ResultPostProcessor<List<String>> {

    private ThreadLocal<Object> threadLocal = new ThreadLocal<>();

    @Override
    public ResultProcessorType getType() {
        return ResultProcessorType.USER_INFO_HANDLER;
    }

    @Override
    public List<String> doProcess(BoundResultRequestContextHolder<List<String>> resultHolder, List<String> handlerResult) {
        System.out.println("handle user info by list which name userId is" + threadLocal.get());
        return handlerResult;
    }

    @Override
    public ThreadLocal<Object> initProcessorTypeContext() {
        threadLocal.set(new UserInfo("admin"));
        return threadLocal;
    }

    static class UserInfo{
        private String userId;

        public UserInfo(String userId) {
            this.userId = userId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        @Override
        public String toString() {
            return "UserInfo{" +
                    "userId='" + userId + '\'' +
                    '}';
        }
    }

}
