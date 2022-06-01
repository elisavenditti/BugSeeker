package org.example;

public class Config {
    private String projName;

    public String getProjName() {
        return projName;
    }

    public String getProjDirName() {
        return projDirName;
    }

    private String projDirName;


    public Config(boolean syncope){
        if(syncope){
            this.projName ="SYNCOPE";
            this.projDirName = "C:\\Users\\Elisa Venditti\\Desktop\\syncope 1.2.10\\syncope";
        }else{
            this.projName ="BOOKKEEPER";
            this.projDirName = "C:\\Users\\Elisa Venditti\\Desktop\\bookkeeper per bugseeker\\bookkeeper";
        }
    }
}
