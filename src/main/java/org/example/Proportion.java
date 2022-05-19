package org.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.example.Jira.getAllRelease;
import static org.example.Jira.getIssueIdOfProject;
import static org.example.Main.*;

public class Proportion {
    private static ArrayList<String> projects;
    private static List<Release> allReleaseToRestore;
    private ArrayList<Integer> countNotNullIVInAllRelease;


    public static void setUpColdStartProjects(){
        projects = new ArrayList<>();

        projects.add("TAJO");
        projects.add("ZOOKEEPER");
        projects.add("STORM");
        projects.add("AVRO");
        projects.add("OPENJPA");
        allReleaseToRestore = Main.getAllRelease();

    }
    public static void restore(){
        Main.setAllRelease(allReleaseToRestore);
    }


    public List<Float> incremental(List<Issue> allIssues, boolean synBook){
        // synBook is true if is SYNCOPE or BOOKKEEPER

        // entry of array is a list of issue fixed in the i-th release
        ArrayList<ArrayList<Issue>> issuePerRelease = new ArrayList<>();
        for(int j=0; j< Main.getAllRelease().size(); j++){
            issuePerRelease.add(new ArrayList<>());
        }
        for(Issue i: allIssues){
            issuePerRelease.get(i.getFixVersion().getIndex()).add(i);
        }


        ArrayList<Float> pinc = new ArrayList<>();
        ArrayList<Integer> countNotNull = new ArrayList<>();
        int count = 0;
        int ivIndex;
        int ovIndex;
        int fvIndex;
        float p;
        float denominatore;
        float incrementalMean = 0;

        for(ArrayList<Issue> issueInRelease: issuePerRelease) {

            if(!issueInRelease.isEmpty()) {
                // calculate p in this release (with all issues fixed here)
                for (Issue iss : issueInRelease) {
                    if (iss.getInjectedVersion() == null) continue;
                    count++;

                    ivIndex = iss.getInjectedVersion().getIndex();
                    fvIndex = iss.getFixVersion().getIndex();
                    ovIndex = iss.getOpeningVersion().getIndex();

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
            Logger logger = Logger.getLogger(Proportion.class.getName());
            for(int pp: countNotNullIVInAllRelease) logger.log(Level.INFO, String.valueOf(pp));
            String out1 = "[Synbook] "+pinc.get(pinc.size()-1);
            String out2 = count+" iv validi";

            logger.log(Level.INFO, out1);
            logger.log(Level.INFO, out2);
        }
        return pinc;
    }



    private float coldStart(){


        ArrayList<Float> pColdStartList = new ArrayList<>();

        setUpColdStartProjects();
        for (String projName: projects){
            try {
                Main.setAllRelease(getAllRelease(projName));
                orderRelease();
                deleteDuplicate();
                List<Issue> allIssues  = getIssueIdOfProject(projName);
                List<Float> pinc = incremental(allIssues, false);
                int lastIndex = pinc.size()-1;
                pColdStartList.add(pinc.get(lastIndex));
                String out = "["+projName+"]: "+pinc.get(lastIndex);
                Logger logger = Logger.getLogger(Proportion.class.getName());
                logger.log(Level.INFO, out);

            } catch (IOException e) {
                Logger logger = Logger.getLogger(Proportion.class.getName());
                logger.log(Level.INFO, e.getMessage());
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

public float globalP(List<Issue> allIssues){

        int count = 0;
        int ivIndex;
        int ovIndex;
        int fvIndex;
        float p;
        float denominatore;
        float incrementalMean = 0;

        // allIssue contains the issues of the whole project
        for(Issue iss: allIssues) {
             // calculate p in this release (with all issues fixed here)
            if (iss.getInjectedVersion() == null) continue;
            count++;

            ivIndex = iss.getInjectedVersion().getIndex();
            fvIndex = iss.getFixVersion().getIndex();
            ovIndex = iss.getOpeningVersion().getIndex();

            if (fvIndex == ovIndex) denominatore = 1;       // per sicurezza
            else denominatore = (float)  fvIndex - ovIndex;
            p = (fvIndex - ivIndex) / denominatore;
            incrementalMean = incrementalMean + (p - incrementalMean) / count;
        }

        return incrementalMean;


    }


    public List<Issue> addMissingInjectedVersions(List<Float> p, List<Issue> allIssue, boolean incremental){
        // Boolean incremental true --> the labeling uses an incremental p (es: P[r-1] to label r)
        // Boolean incremental false --> the labeling uses the global p

        int ivIndex;
        int ovIndex;
        int fvIndex;
        float pColdStart = coldStart();
        float pToUse;

        for(Issue iss: allIssue) {

            if(iss.getToLabel()) {


                fvIndex = iss.getFixVersion().getIndex();
                ovIndex = iss.getOpeningVersion().getIndex();

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
                iss.setInjectedVersion(Main.getAllRelease().get(ivIndex));
            }
        }
        return allIssue;

    }
}
