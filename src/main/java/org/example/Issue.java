package org.example;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.example.Main.allRelease;

public class Issue {

    private String key;
    private List<Release> jiraAffectedVersions;
    private Date fixDate;
    private Date creationDate;
    private Release fixVersion;
    private Release openingVersion;
    private Release injectedVersion;
    private Boolean toLabel;
    public String getKey() {
        return key;
    }

    public Release getFixVersion() {
        return fixVersion;
    }

    public Release getOpeningVersion() {
        return openingVersion;
    }

    public Release getInjectedVersion() {
        return injectedVersion;
    }

    public void setInjectedVersion(Release injectedVersion) {
        this.injectedVersion = injectedVersion;
    }

    public Boolean getToLabel() {
        return toLabel;
    }



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


    private List<Release> getReleaseArrayFromStringArray(List<String> stringArray){
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
        if(this.injectedVersion != null && this.openingVersion !=null && this.injectedVersion.index > this.openingVersion.index) {
                this.injectedVersion = null;
                this.toLabel = true;
            }
    }

    private Release getMinRelease(List<Release> releases){
        Release ret = null;
        for(Release r: releases){
            if(ret==null || r.releaseDate.before(ret.releaseDate)) ret=r;
        }
        return ret;
    }


    public Issue(String key,List<String> affectedVersions, String resolutionDate, String creationDate) {

        this.toLabel = false;       // default value
        this.key = key;
        this.jiraAffectedVersions = getReleaseArrayFromStringArray(affectedVersions);

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String dateSubtring;
        try{
            dateSubtring = resolutionDate.substring(0, 10);
            this.fixDate = df.parse(dateSubtring);
            dateSubtring = creationDate.substring(0, 10);
            this.creationDate = df.parse(dateSubtring);

        } catch (ParseException e) {
            Logger logger = Logger.getLogger(Issue.class.getName());
            logger.log(Level.INFO, e.getMessage());
        }


        this.fixVersion = this.getReleaseFromDate(this.fixDate);
        this.openingVersion = this.getReleaseFromDate(this.creationDate);

        if(this.jiraAffectedVersions.isEmpty()) {
            this.injectedVersion = this.getMinRelease(jiraAffectedVersions);
        }
        else {
            this.injectedVersion = null;
            this.toLabel = true;
        }
        // se IV > OV le Affected Version su jira erano inconsistenti quindi le devo ricalcolare con proportion
        consistencyCheck();

    }

}
