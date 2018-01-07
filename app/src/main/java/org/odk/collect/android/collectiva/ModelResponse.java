package org.odk.collect.android.collectiva;

import java.util.Objects;

/**
 * Created by lenovo on 5/1/2017.
 */

public class ModelResponse {
    public ModelResponse(boolean isSuccess, int code, String message) {
        this.isSuccess = isSuccess;
        this.code = code;
        this.message = message;
    }
    boolean isSuccess;
    int code;
    String message;

    public Object getRespons() {
        return respons;
    }

    public void setRespons(Object respons) {
        this.respons = respons;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    Object respons;
}
