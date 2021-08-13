package com.gantt.rest;

import java.util.ArrayList;

public class TaskObject {
    private String id;
    private String name;
    private String start;
    private String end;
    private int progress;
    private ArrayList<String> dependencies;

    public TaskObject(String id, String name, String start, String end, int progress, ArrayList<String> dependencies){
        this.id = id;
        this.name = name;
        this.start = start;
        this.end = end;
        this.progress = progress;
        this.dependencies = dependencies;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getStart() {
        return start;
    }

    public String getEnd() {
        return end;
    }

    public int getProgress() {
        return progress;
    }

    public ArrayList<String> getDependencies() {
        return dependencies;
    }
}
