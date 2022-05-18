package org.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static org.example.Jira.getAllRelease;
import static org.example.Jira.getIssueIdOfProject;
import static org.example.Main.*;

public class Proportion {
    private static ArrayList<String> projects;
    private static ArrayList<Release> allReleaseToRestore;
    private static ArrayList<Integer> countNotNullIVInAllRelease;


    public static void setUpColdStartProjects(){
        projects = new ArrayList<>();

        projects.add("TAJO");
        projects.add("ZOOKEEPER");
        projects.add("STORM");
        projects.add("AVRO");
        projects.add("OPENJPA");
        allReleaseToRestore = allRelease;

    }
    public static void restore(){
        allRelease = allReleaseToRestore;
    }


    public ArrayList<Float> incremental(ArrayList<Issue> allIssues, Boolean synBook){
        // synBook is true if is SYNCOPE or BOOKKEEPER

        // entry of array is a list of issue fixed in the i-th release
        ArrayList<ArrayList<Issue>> issuePerRelease = new ArrayList<>();
        for(int j=0; j< allRelease.size(); j++){
            issuePerRelease.add(new ArrayList<>());
        }
        for(Issue i: allIssues){
            issuePerRelease.get(i.fixVersion.index).add(i);
        }


        ArrayList<Float> pinc = new ArrayList<>();
        ArrayList<Integer> countNotNull = new ArrayList<>();
        int count = 0, ivIndex, ovIndex, fvIndex;
        float p, denominatore;
        float incrementalMean = 0;

        for(ArrayList<Issue> issueInRelease: issuePerRelease) {

            if(issueInRelease.size()!=0) {
                // calculate p in this release (with all issues fixed here)
                for (Issue iss : issueInRelease) {
                    if (iss.injectedVersion == null) continue;
                    count++;

                    ivIndex = iss.injectedVersion.index;
                    fvIndex = iss.fixVersion.index;
                    ovIndex = iss.openingVersion.index;

                    if (fvIndex == ovIndex) denominatore = 1;
                    else denominatore = (float) fvIndex - ovIndex;
                    p = (fvIndex - ivIndex) / denominatore;
                    incrementalMean = incrementalMean + (p - incrementalMean) / count;
                }
            }

            countNotNull.add(count);    // = not null IV found in the release i-th
            if(synBook) countNotNullIVInAllRelease = countNotNull;
            pinc.add(incrementalMean);

        }
        if(synBook) {
            for(int pp: countNotNullIVInAllRelease) System.out.println(pp);
            System.out.println("[Synbook] "+pinc.get(pinc.size()-1));
            System.out.println(count+" iv validi");
        }
        return pinc;
    }



    private float coldStart(){


        ArrayList<Float> pColdStartList = new ArrayList<>();

        setUpColdStartProjects();
        for (String projName: projects){
            try {
                allRelease = getAllRelease(projName);
                orderRelease();
                deleteDuplicate();
                ArrayList<Issue> allIssues  = getIssueIdOfProject(projName);
                ArrayList<Float> pinc = incremental(allIssues, false);
                int lastIndex = pinc.size()-1;
                pColdStartList.add(pinc.get(lastIndex));
                System.out.println("["+projName+"]: "+pinc.get(lastIndex));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        Collections.sort(pColdStartList);
        float median;
        int len = pColdStartList.size();
        if (len % 2 == 0)
            median = (pColdStartList.get(len/2) + pColdStartList.get(len/2-1))/2;
        else
            median = pColdStartList.get(len/2);

        restore();
        return median;
    }

public float globalP(ArrayList<Issue> allIssues){

        int count = 0, ivIndex, ovIndex, fvIndex;
        float p, denominatore;
        float incrementalMean = 0;

        // allIssue contains the issues of the whole project
        for(Issue iss: allIssues) {
             // calculate p in this release (with all issues fixed here)
            if (iss.injectedVersion == null) continue;
            count++;

            ivIndex = iss.injectedVersion.index;
            fvIndex = iss.fixVersion.index;
            ovIndex = iss.openingVersion.index;

            if (fvIndex == ovIndex) denominatore = 1;       // per sicurezza
            else denominatore = (float)  fvIndex - ovIndex;
            p = (fvIndex - ivIndex) / denominatore;
            incrementalMean = incrementalMean + (p - incrementalMean) / count;
        }

        return incrementalMean;


    }


    public ArrayList<Issue> addMissingInjectedVersions(ArrayList<Float> p, ArrayList<Issue> allIssue, Boolean incremental){
        // Boolean incremental true --> the labeling uses an incremental p (es: P[r-1] to label r)
        // Boolean incremental false --> the labeling uses the global p

        int ivIndex, ovIndex, fvIndex;
        float pColdStart = coldStart();
        float pToUse;

        for(Issue iss: allIssue) {

            if(iss.toLabel) {


                fvIndex = iss.fixVersion.index;
                ovIndex = iss.openingVersion.index;

                int diff;
                if (fvIndex==ovIndex)       // per sicurezza
                    diff = 1;
                else
                    diff= fvIndex-ovIndex;

                // devo usare il Pincrement calcolato da 0 a R-1
                // ma se ho meno di 5 issue devo usare coldStart

                if(incremental && countNotNullIVInAllRelease.get(fvIndex)<5)
                    pToUse = pColdStart;
                else if(!incremental){
                    // nel relabel devo fare il labeling usando il p globale
                    // calcolato da tutte le issue:
                    pToUse = p.get(p.size()-1);
                }
                else
                    pToUse = p.get(fvIndex-1);

                ivIndex = fvIndex - Math.round(diff*pToUse);
                if(ivIndex<0) ivIndex = 0;
                iss.injectedVersion = allRelease.get(ivIndex);
            }
        }
        return allIssue;

    }
}
