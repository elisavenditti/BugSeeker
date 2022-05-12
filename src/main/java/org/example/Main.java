package org.example;

import org.json.JSONException;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static org.example.Jira.*;

public class Main {

    public static ArrayList<Release> allRelease;
    public static ArrayList<Release> halfRelease;
    public static Boolean syncope = true;
    public static int rootLen;

    private static boolean isReleaseContainedIn(Release currRelease, Release iv, Release  fv, Boolean extremeIncluded){
        int ivIndex = iv.index;
        int fvIndex = fv.index;
        int i= currRelease.index;
        boolean contained = false;

        if(extremeIncluded && i>=ivIndex && i<=fvIndex) contained = true;
        else if(!extremeIncluded && i>=ivIndex && i<fvIndex) contained = true;
        return contained;
    }


    private static ArrayList<Float> proportion(ArrayList<Issue> allIssues){
        ArrayList<ArrayList<Issue>> issuePerRelease = new ArrayList<>();
        for(int j=0; j< allRelease.size(); j++){
            issuePerRelease.add(new ArrayList<>());
        }
        for(Issue i: allIssues){
            issuePerRelease.get(i.fixVersion.index).add(i);
        }


        ArrayList<Float> pinc = new ArrayList<>();
        int count = 0;
        float p, denominatore;
        float incrementalMean = 0;
        int ivIndex, ovIndex, fvIndex;
        for(ArrayList<Issue> issueInRelease: issuePerRelease) {
            if(issueInRelease.size()!=0) {
                // calcola p per questa versione
                for (Issue iss : issueInRelease) {
                    if (iss.injectedVersion == null) continue;
                    count++;

                    ivIndex = iss.injectedVersion.index;
                    fvIndex = iss.fixVersion.index;
                    ovIndex = iss.openingVersion.index;

                    if (fvIndex == ovIndex) denominatore = 1;
                    else denominatore = fvIndex - ovIndex;
                    p = (fvIndex - ivIndex) / denominatore;
//                    System.out.println(count+") iv="+ivIndex+" - ov="+ovIndex+" - fv="+fvIndex+" - p="+p);
                    incrementalMean = incrementalMean + (p - incrementalMean) / count;
                }
            }

            pinc.add(incrementalMean);

        }
        for(Float pp: pinc)System.out.println(pp);
        return pinc;
    }

    private static void addMissingInjectedVersions(/*ArrayList<Float>*/ float p, ArrayList<Issue> allIssue){
        int ivIndex, ovIndex, fvIndex;

        for(Issue iss: allIssue){
            if(iss.injectedVersion == null) {

                fvIndex = iss.fixVersion.index;
                ovIndex = iss.openingVersion.index;
                int diff;
                if (fvIndex==ovIndex) diff = 1;
                else diff= fvIndex-ovIndex;
                //float pToUse = p.get(fvIndex);
                ivIndex = fvIndex - Math.round(diff*p/*pToUse*/);
                System.out.println("iv: "+ivIndex+" fv: "+fvIndex+" ov: "+ovIndex);
                if(ivIndex<0) ivIndex = 0;
                iss.injectedVersion = allRelease.get(ivIndex);
            }
        }

    }




    public static void main(String[] args) throws IOException, JSONException, ParseException {

//        String projName ="SYNCOPE";
//        String projDirName = "C:\\Users\\Elisa Venditti\\Desktop\\syncope 1.2.10\\syncope";
        String projName ="BOOKKEEPER";
        String projDirName = "C:\\Users\\Elisa Venditti\\Desktop\\bookkeeper per bugseeker\\bookkeeper";
        Excel excel = new Excel(projDirName);
        String rootDir = projDirName+"\\";
        rootLen = rootDir.length();


        allRelease = getAllRelease(projName);
        orderRelease();
//        if(syncope){
//            allRelease.add(new Release("3.0.0", longDate));
//        }
        halfRelease = getHalfReleases(allRelease);


//        for(Release r: halfRelease)
//            System.out.println(r.index+") "+r.name);


        ArrayList<Issue> allIssues  = getIssueIdOfProject(projName);


        ArrayList<Float> pinc = proportion(allIssues);
        int size=pinc.size();
        addMissingInjectedVersions(pinc.get(size-1), allIssues);


        ArrayList<Issue> valuableIssue = new ArrayList<>();
        for(Issue i: allIssues){
            // delete issue with IV > halfRelease
            if(i.injectedVersion.index<=halfRelease.size()-1) valuableIssue.add(i);

        }
        System.out.println("|valuableIssue|="+valuableIssue.size());

//        Git github = new Git(projDirName + "\\.git", true);
        Git github = new Git(projDirName + "\\.git", false);
        github.getReleaseFileList(projName, excel, projDirName);

        ArrayList<Commit> commit_id = new ArrayList<>();
        int k=0;
        for(Issue i: valuableIssue){
            System.out.println("* search commit in issue "+k);
//            List<Commit> list = github.getAllCommitsOfIssue(i, true);
            List<Commit> list = github.getAllCommitsOfIssue(i, false);
            commit_id.addAll(list);
            k++;

        }
        System.out.println("|COMMIT|="+commit_id.size());
        github.getMetrics();

//        System.exit(-200);
        for(Release release: halfRelease) {

            System.out.println("inizializzo excel per: "+release.name);
            for (Issue i : valuableIssue) {
                if(isReleaseContainedIn(release, i.injectedVersion, i.fixVersion, false)) {
                    for(Commit com: commit_id){

                        if(isReleaseContainedIn(com.release, release.next(), i.fixVersion, true)){
                            if(com.changedFiles!=null && com.changedFiles.size()>0)
                                release.buggyFiles.addAll(com.changedFiles);
                        }


                    }
                }
            }
        }


        excel.populate(projName);

    }

    private static void orderRelease() {
        ArrayList<Release> orderedRelease = new ArrayList<>();
        for(Release rr: allRelease){
            if (orderedRelease.size()==0)
                orderedRelease.add(rr);
            else{
                int count=0;
                while(count< orderedRelease.size()){
                    if((orderedRelease.get(count).releaseDate).after(rr.releaseDate)) break;
                    count++;
                }

                if(count==orderedRelease.size())
                    orderedRelease.add(rr);
                else
                    orderedRelease.add(count,rr);

            }

        }
        int z=0;
        for(Release r: orderedRelease){
            r.index=z;
            z++;
        }

        allRelease = orderedRelease;
    }


}
