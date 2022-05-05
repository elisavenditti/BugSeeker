package org.example;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static org.example.Main.allRelease;

public class Release {

    public String name;
    public Date releaseDate;
    public ArrayList<String> files;
    public ArrayList<String> buggyFiles;

    public Release next(){
        int i=0;
        for (Release r: allRelease){
            if(this.name.equalsIgnoreCase(r.name)) break;
            i++;
        }
        return allRelease.get(i+1);

    }
    public Release(String name, String releaseDate){
        this.name = name;
        this.buggyFiles = new ArrayList<>();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String releaseDateString;
        try{
            releaseDateString = releaseDate.substring(0, 10);
            this.releaseDate = df.parse(releaseDateString);

        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
