package com.proper.data;

import java.io.Serializable;

/**
 * Created by Lebel on 17/02/2015.
 */
public class LastQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    private String Bin;
    Double Duration;

    public LastQuery() {
    }

    public LastQuery(String bin, Double duration) {
        Bin = bin;
        Duration = duration;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getBin() {
        return Bin;
    }

    public void setBin(String bin) {
        Bin = bin;
    }

    public Double getDuration() {
        return Duration;
    }

    public void setDuration(Double duration) {
        Duration = duration;
    }
}
