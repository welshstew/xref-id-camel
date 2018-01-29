package com.nullendpoint.xref;

/**
 * Created by swinches on 07/06/17.
 */
public class SimpleResponse {
    private String status;

    public SimpleResponse(){}

    public SimpleResponse(String status){
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
