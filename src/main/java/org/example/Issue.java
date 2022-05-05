package org.example;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import static org.example.Main.allRelease;

public class Issue {

    public String key;
    public ArrayList<String> jiraFixVersions;
    public ArrayList<String> affectedVersions;

    public Date fixDate;
    public String uniqueFixVersion;
    public String injectedVersion;
    public Date injectedDate;

    private String getFixVersion(){
        String definitiveFV = "0.0.0";


        for (String fv: this.jiraFixVersions){
            int i=0;
            String[] partsFV = definitiveFV.split("\\.");
            String[] parts = fv.split("\\.");
            Boolean greater = false;
            for(String p: partsFV){

                if(Integer.parseInt(p) < Integer.parseInt(parts[i])){
                    greater = true;
                    definitiveFV = fv;
                    break;
                }else if(Integer.parseInt(p) > Integer.parseInt(parts[i])){
                    greater = false;
                    break;
                }
                i++;
            }
        }

        return definitiveFV;
    }




    private String getInjectedVersion(){
        int minIndex = -1;
        for (Release r: allRelease) {
            if (this.affectedVersions.contains(r.name)) {
                minIndex = this.affectedVersions.indexOf(r.name);
                break;
            }
        }
        if (this.affectedVersions.contains("3.0.0")) return "3.0.0";
        if (this.affectedVersions.size()==0) return "0.0.0";
        if (this.affectedVersions.contains("1.1.9") && this.affectedVersions.size()==1) return "1.1.8";
        System.out.println(this.affectedVersions);
        return allRelease.get(minIndex).name;
    }
    public Issue(String key, ArrayList<String> fixVersions, ArrayList<String> affectedVersions, String resolutionDate) {
        this.key = key;
        this.jiraFixVersions = fixVersions;
        this.affectedVersions = affectedVersions;

        this.uniqueFixVersion = this.getFixVersion();
        this.injectedVersion = this.getInjectedVersion(); // TO DO: with proportion!


        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String fixDateString;
        try{
            fixDateString = resolutionDate.substring(0, 10);
            this.fixDate = df.parse(fixDateString);

        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

}
