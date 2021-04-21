package com.edu.pollingquery.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 查询后结果包装
 *
 * @author jcb
 * @since 2021/1/22
 */
public class ResultWrapper<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private Date startTime;

    private String actionCode;

    private T result;

    public ResultWrapper() {
    }

    public ResultWrapper(Date startTime, String actionCode, T result) {
        this.startTime = startTime;
        this.actionCode = actionCode;
        this.result = result;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public String getActionCode() {
        return actionCode;
    }

    public void setActionCode(String actionCode) {
        this.actionCode = actionCode;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }
}
