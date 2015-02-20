package com.proper.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Created by Lebel on 20/02/2015.
 */
public class ServerResponseObject implements Serializable {
    private static final long serialVersionUID = 1L;
    private String Response;
    private Double Duration;

    public ServerResponseObject() {
    }

    public ServerResponseObject(String response, Double duration) {
        Response = response;
        Duration = duration;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    @JsonProperty("Response")
    public String getResponse() {
        return Response;
    }

    public void setResponse(String response) {
        Response = response;
    }

    @JsonProperty("Duration")
    public Double getDuration() {
        return Duration;
    }

    public void setDuration(Double duration) {
        Duration = duration;
    }
}
