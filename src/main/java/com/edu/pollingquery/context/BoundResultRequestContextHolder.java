package com.edu.pollingquery.context;

import com.edu.pollingquery.processor.ResultProcessorType;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * 带有请求发起时间、用户信息的延迟结果
 *
 * @author jcb
 * @since 2021/1/21
 */
public class BoundResultRequestContextHolder<T> extends DeferredResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 返回结果视图编码
     */
    private String viewCode;

    /**
     * 请求发起时间
     */
    private volatile Date reqStartTime;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 部门ID
     */
    private String deptId;

    /**
     * 依赖的处理方法
     */
    private Function<Object, T> function;

    /**
     * 依赖的处理方法
     */
    private Supplier<T> supplier;

    /**
     * 处理方法参数
     */
    private Object param;

    /**
     * 处理器类型
     */
    private Set<ResultProcessorType> supportProcessorTypes;

    /**
     * 请求默认超时时间
     */
    private static final Long DEFAULT_TIMEOUT = 5000L;

    public BoundResultRequestContextHolder(String viewCode, Function<Object, T> function, Object param,
                                           String userId, String deptId) {
        super(DEFAULT_TIMEOUT);
        this.reqStartTime = new Date();
        this.viewCode = viewCode;
        this.function = checkNotNull(function);
        this.param = param;
        this.userId = userId;
        this.deptId = deptId;
    }

    public BoundResultRequestContextHolder(String viewCode, Supplier<T> supplier, String userId, String deptId) {
        super(DEFAULT_TIMEOUT);
        this.reqStartTime = new Date();
        this.viewCode = viewCode;
        this.supplier = checkNotNull(supplier);
        this.userId = userId;
        this.deptId = deptId;
    }

    public BoundResultRequestContextHolder(String viewCode, Date reqStartTime, String userId, String deptId,
                                           Function<Object, T> function, Object param, Long timeout,
                                           Set<ResultProcessorType> supportProcessorTypes) {
        super(timeout != null ? timeout : DEFAULT_TIMEOUT);
        this.viewCode = viewCode;
        this.reqStartTime = reqStartTime;
        this.userId = userId;
        this.deptId = deptId;
        this.function = checkNotNull(function);
        this.param = param;
        this.supportProcessorTypes = supportProcessorTypes;
    }

    public BoundResultRequestContextHolder(String viewCode, Date reqStartTime, String userId, String deptId,
                                           Supplier<T> supplier, Long timeout,
                                           Set<ResultProcessorType> supportProcessorTypes) {
        super(timeout != null ? timeout : DEFAULT_TIMEOUT);
        this.viewCode = viewCode;
        this.reqStartTime = reqStartTime;
        this.userId = userId;
        this.deptId = deptId;
        this.supplier = checkNotNull(supplier);
        this.supportProcessorTypes = supportProcessorTypes;
    }

    public BoundResultRequestContextHolder(String viewCode, String userId, String deptId,
                                           Supplier<T> supplier, Long timeout,
                                           Set<ResultProcessorType> supportProcessorTypes) {
        super(timeout != null ? timeout : DEFAULT_TIMEOUT);
        this.viewCode = viewCode;
        this.reqStartTime = new Date();
        this.userId = userId;
        this.deptId = deptId;
        this.supplier = checkNotNull(supplier);
        this.supportProcessorTypes = supportProcessorTypes;
    }

    public BoundResultRequestContextHolder(Long timeout, Object timeoutResult, String viewCode,
                                           Date reqStartTime, String userId, String deptId,
                                           Function<Object, T> function, Object param,
                                           Set<ResultProcessorType> supportProcessorTypes) {
        super(timeout, timeoutResult);
        this.viewCode = viewCode;
        this.reqStartTime = reqStartTime;
        this.userId = userId;
        this.deptId = deptId;
        this.function = checkNotNull(function);
        this.param = param;
        this.supportProcessorTypes = supportProcessorTypes;
    }

    public BoundResultRequestContextHolder(Long timeout, Object timeoutResult, String viewCode,
                                           Date reqStartTime, String userId, String deptId, Supplier<T> supplier,
                                           Set<ResultProcessorType> supportProcessorTypes) {
        super(timeout, timeoutResult);
        this.viewCode = viewCode;
        this.reqStartTime = reqStartTime;
        this.userId = userId;
        this.deptId = deptId;
        this.supplier = checkNotNull(supplier);
        this.supportProcessorTypes = supportProcessorTypes;
    }

    public String getViewCode() {
        return viewCode;
    }

    public void setViewCode(String viewCode) {
        this.viewCode = viewCode;
    }

    public Date getReqStartTime() {
        return reqStartTime;
    }

    public void setReqStartTime(Date reqStartTime) {
        this.reqStartTime = reqStartTime;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public Function<Object, T> getFunction() {
        return function;
    }

    public void setFunction(Function<Object, T> function) {
        this.function = function;
    }

    public Object getParam() {
        return param;
    }

    public void setParam(Object param) {
        this.param = param;
    }

    public Supplier<T> getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public Set<ResultProcessorType> getSupportProcessorTypes() {
        return supportProcessorTypes;
    }

    public void setSupportProcessorTypes(Set<ResultProcessorType> supportProcessorTypes) {
        this.supportProcessorTypes = supportProcessorTypes;
    }

    @Override
    public String toString() {
        return "BoundResultRequestContextHolder{" +
                "viewCode='" + viewCode + '\'' +
                ", reqStartTime=" + reqStartTime +
                ", userId='" + userId + '\'' +
                ", deptId='" + deptId + '\'' +
                ", function=" + function +
                ", supplier=" + supplier +
                ", param=" + param +
                ", supportProcessorTypes=" + supportProcessorTypes +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BoundResultRequestContextHolder<?> that = (BoundResultRequestContextHolder<?>) o;
        return Objects.equals(viewCode, that.viewCode) &&
                Objects.equals(reqStartTime, that.reqStartTime) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(deptId, that.deptId) &&
                Objects.equals(function, that.function) &&
                Objects.equals(supplier, that.supplier) &&
                Objects.equals(param, that.param) &&
                Objects.equals(supportProcessorTypes, that.supportProcessorTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(viewCode, reqStartTime, userId, deptId, function, supplier, param, supportProcessorTypes);
    }
}