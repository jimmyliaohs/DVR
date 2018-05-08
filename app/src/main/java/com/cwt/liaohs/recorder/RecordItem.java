package com.cwt.liaohs.recorder;

public class RecordItem {
    private int record_id;
    private int record_lock;
    private String record_name;
    private int record_resolution;

    public int getRecord_id() {
        return record_id;
    }

    public void setRecord_id(int record_id) {
        this.record_id = record_id;
    }

    public int getRecord_lock() {
        return record_lock;
    }

    public void setRecord_lock(int record_lock) {
        this.record_lock = record_lock;
    }

    public String getRecord_name() {
        return record_name;
    }

    public void setRecord_name(String record_name) {
        this.record_name = record_name;
    }

    public int getRecord_resolution() {
        return record_resolution;
    }

    public void setRecord_resolution(int record_resolution) {
        this.record_resolution = record_resolution;
    }


}
