package org.example;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Commit {
    private String commitId;
    private Date commitDate;

    private Release release;

    public String getAuthor() {
        return author;
    }

    private String author;
    private int releaseIndex;
    private List<MyFile> changedFiles;

    private Release getReleaseFromDate(){
        int i=0;
        for(Release r: Main.getAllRelease()){
            if(this.commitDate.before(r.releaseDate)) break;
            i++;
        }
        int size = Main.getAllRelease().size();
        if (i==size) {
            this.releaseIndex=-1;
            return null;        //il commit appartiene alla release corrente (che ancora deve uscire)
        }
        this.releaseIndex = i;
        return Main.getAllRelease().get(i);
    }

    public String getCommitSha(){

        String[] parts = this.commitId.split(" ");
        return parts[1];
    }


    public Commit(String cid, String author, Date data){


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


    public Release getRelease() {
        return release;
    }

    public void setRelease(Release release) {
        this.release = release;
    }
    public void setAuthor(String author) {
        this.author = author;
    }

    public int getReleaseIndex() {
        return releaseIndex;
    }

    public void setReleaseIndex(int releaseIndex) {
        this.releaseIndex = releaseIndex;
    }

    public List<MyFile> getChangedFiles() {
        return changedFiles;
    }

    public void setChangedFiles(List<MyFile> changedFiles) {
        this.changedFiles = changedFiles;
    }

}
