package org.example;
import java.util.ArrayList;
import java.util.Date;

import static org.example.Main.allRelease;

public class Commit {
    private String commitId;
    private Date commitDate;
    public Release release;
    public String author;
    public int releaseIndex;
    private Issue issue;
    public ArrayList<MyFile> changedFiles;

    private Release getReleaseFromDate(){
        int i=0;
        for(Release r: allRelease){
            if(this.commitDate.before(r.releaseDate)) break;
            i++;
        }
        int size = allRelease.size();
        if (i==size) {
            this.releaseIndex=-1;
            return null;        //il commit appartiene alla release corrente (che ancora deve uscire)
        }
        this.releaseIndex = i;
        return allRelease.get(i);
    }

    public String getCommitSha(){

        String[] parts = this.commitId.split(" ");
        return parts[1];
    }


    public Commit(String cid, String author, Date data, Issue issue){


        this.issue = issue;
        this.commitId = cid;
        this.author = author;
        this.commitDate = data;
        this.release = getReleaseFromDate();
        this.changedFiles = new ArrayList<>();


    }
    public Commit(String cid){
        this.commitId = cid;
        this.changedFiles = new ArrayList<>();
    }

}
