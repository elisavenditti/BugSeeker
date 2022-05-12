package org.example;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static org.example.Main.allRelease;
import static org.example.Main.rootLen;

public class Release {

    public String name;
    public Date releaseDate;
    public ArrayList<MyFile> files;
    public ArrayList<MyFile> filesWithMetric;
    public ArrayList<MyFile> buggyFiles;
    public int index;

    public Release next(){
        return allRelease.get(index+1);

    }

    public Release(String name, String releaseDate){
        this.name = name;
        this.buggyFiles = new ArrayList<>();
        this.files = new ArrayList<>();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String releaseDateString;
        this.index=-1;
        this.filesWithMetric = new ArrayList<>();
        try{
            releaseDateString = releaseDate.substring(0, 10);
            this.releaseDate = df.parse(releaseDateString);

        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
