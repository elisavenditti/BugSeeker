package org.example;

import java.util.ArrayList;
import java.util.List;

public class MyFile {
    private String pathname;
    private int locTouched;
    private int loc;
    private int locAdded;
    private int chgSetSize;
    private long nAuthors;
    private List<String> authors;
    private List<Integer> locAddedList;
    private List<Integer> chgSetSizeList;
    private int nRevisions;
    public String getPathname() {
        return pathname;
    }

    public int getLocTouched() {
        return locTouched;
    }

    public void setLocTouched(int locTouched) {
        this.locTouched = locTouched;
    }

    public int getLoc() {
        return loc;
    }

    public void setLoc(int loc) {
        this.loc = loc;
    }

    public int getLocAdded() {
        return locAdded;
    }

    public void setLocAdded(int locAdded) {
        this.locAdded = locAdded;
    }

    public int getChgSetSize() {
        return chgSetSize;
    }

    public void setChgSetSize(int chgSetSize) {
        this.chgSetSize = chgSetSize;
    }

    public long getnAuthors() {

        nAuthors = getNumberOfAuthors();
        return nAuthors;
    }

    public List<String> getAuthors() {
        return authors;
    }
    public void addItemInAuthors(String item){this.authors.add(item);}

    public List<Integer> getLocAddedList() {
        return locAddedList;
    }

    public void addItemInLocAddedList(int item) {
        this.locAddedList.add(item);
    }

    public List<Integer> getChgSetSizeList() {
        return chgSetSizeList;
    }

    public void addItemInChgSetSizeList(int item) {
        this.chgSetSizeList.add(item);
    }

    public int getnRevisions() {
        return nRevisions;
    }

    public void setnRevisions(int nRevisions) {
        this.nRevisions = nRevisions;
    }



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
        this.locAdded = 0;
        this.locTouched = 0;
        this.chgSetSize = 0;
        this.chgSetSizeList = new ArrayList<>();
        this.locAddedList = new ArrayList<>();
    }






}
