package org.example;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import static org.example.Main.allRelease;

public class Issue {

    public String key;
    public ArrayList<Release> jiraFixVersions;
    public ArrayList<Release> jiraAffectedVersions;

    public Date fixDate;
    public Date creationDate;
    public Release fixVersion;
    public Release openingVersion;
    public Release injectedVersion;
    public Date injectedDate;
    public Boolean toLabel;


    private Release getReleaseFromDate(Date date){
        int i=0;
        for(Release r: allRelease){
            if(date.before(r.releaseDate)) break;
            i++;
        }
        int size = allRelease.size();
        if (i==size) return null;
        return allRelease.get(i);
    }


    private ArrayList<Release> getReleaseArrayFromStringArray(ArrayList<String> stringArray){
        ArrayList<Release> releaseArray = new ArrayList<>();

        for(Release r: allRelease){             // il for esterno fa in modo che la lista venga
                                                // costruita in ordine cronologico
            for(String relString: stringArray){
                if(r.name.equalsIgnoreCase(relString))
                    releaseArray.add(r);
            }
        }
        return releaseArray;
    }

    private void consistencyCheck(){
        if(this.injectedVersion != null && this.openingVersion !=null)
            if(this.injectedVersion.index > this.openingVersion.index) {
                this.injectedVersion = null;
                this.toLabel = true;
            }
    }

    private Release getMinRelease(ArrayList<Release> releases){
        Release ret = null;
        for(Release r: releases){
            if(ret==null) ret = r;
            else if(r.releaseDate.before(ret.releaseDate)) ret=r;
        }
        return ret;
    }


    public Issue(String key, ArrayList<String> fixVersions, ArrayList<String> affectedVersions, String resolutionDate, String creationDate) {

        this.toLabel = false;       // default value
        this.key = key;
        this.jiraFixVersions = getReleaseArrayFromStringArray(fixVersions);
        this.jiraAffectedVersions = getReleaseArrayFromStringArray(affectedVersions);

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String dateSubtring;
        try{
            dateSubtring = resolutionDate.substring(0, 10);
            this.fixDate = df.parse(dateSubtring);
            dateSubtring = creationDate.substring(0, 10);
            this.creationDate = df.parse(dateSubtring);

        } catch (ParseException e) {
            throw new RuntimeException(e);
        }


        //this.fixVersion = getMaxMinRelease(this.jiraFixVersions, true);
        this.fixVersion = this.getReleaseFromDate(this.fixDate);
        this.openingVersion = this.getReleaseFromDate(this.creationDate);

        if(this.jiraAffectedVersions.size()!=0) {
            this.injectedVersion = this.getMinRelease(jiraAffectedVersions);
            if(this.injectedVersion!=null) this.injectedDate = this.injectedVersion.releaseDate;
        }
        else {
            this.injectedVersion = null;
            this.toLabel = true;
        }
        // se IV > OV le Affected Version su jira erano inconsistenti quindi le devo ricalcolare con proportion
        consistencyCheck();

    }

}
