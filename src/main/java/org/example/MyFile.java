package org.example;

import java.util.ArrayList;

public class MyFile {

    public String pathname;
    public int locTouched;
    public int loc;
    public int locAdded;
    public int chgSetSize;
    public long nAuthors;
    public ArrayList<String> authors;
    public int nSmells;
    public ArrayList<Integer> locAddedList;
    public ArrayList<Integer> chgSetSizeList;
    public int nRevisions;

    public long getNumberOfAuthors(){
        long unique = authors.stream().distinct().count();
        this.nAuthors = unique;
        return unique;
    }


    public MyFile(String file){
        this.pathname = file;
        this.authors = new ArrayList<>();
        this.nRevisions=0;
        this.nAuthors=0;
        this.nSmells=0;
        this.locAdded = 0;
        this.locTouched = 0;
        this.chgSetSize = 0;
        this.chgSetSizeList = new ArrayList<>();
        this.locAddedList = new ArrayList<>();
    }






}
