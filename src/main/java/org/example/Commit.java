package org.example;
import java.util.ArrayList;
import java.util.Date;

import static org.example.Main.allRelease;

public class Commit {
    private String commitId;
    private Date commitDate;
    public Release release;
    private Issue issue;
    public ArrayList<String> changedFiles;

    private Release getReleaseFromDate(){
        int i=0;
        for(Release r: allRelease){
            if(this.commitDate.before(r.releaseDate)) break;
            i++;
        }
        int size = allRelease.size();
        if (i==size) return allRelease.get(size-1);
        return allRelease.get(i);
    }

    public String getCommitSha(){

        String[] parts = this.commitId.split(" ");
        return parts[1];
    }


    public Commit(String cid, Date data, Issue issue){
        this.issue = issue;
        this.commitId = cid;
        this.commitDate = data;
        this.release = getReleaseFromDate();
        this.changedFiles = new ArrayList<>();

    }

}
