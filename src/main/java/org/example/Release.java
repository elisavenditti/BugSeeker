package org.example;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Release {

    private String name;
    private Date releaseDate;
    private List<MyFile> files;
    private List<MyFile> buggyFiles;
    private int index;

    public String getName() {
        return name;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public List<MyFile> getFiles() {
        return files;
    }

    public void setFiles(List<MyFile> files) {
        this.files = files;
    }


    public List<MyFile> getBuggyFiles() {
        return buggyFiles;
    }

    public void addAllBuggyFiles(List<MyFile> buggyFiles) {
        this.buggyFiles.addAll(buggyFiles);
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Release next(){
        return Main.getAllRelease().get(index+1);

    }

        public Release(String name, String releaseDate){
        this.name = name;
        this.buggyFiles = new ArrayList<>();
        this.files = new ArrayList<>();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String releaseDateString;
        this.index=-1;
        try{
            releaseDateString = releaseDate.substring(0, 10);
            this.releaseDate = df.parse(releaseDateString);

        } catch (ParseException e) {
            Logger logger = Logger.getLogger(Release.class.getName());
            logger.log(Level.INFO, e.getMessage());
        }
    }
}
