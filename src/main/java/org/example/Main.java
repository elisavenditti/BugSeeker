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
    public static String longDate = "2050-12-30";

    private static boolean isReleaseContainedIn(String currRelease, String iv, String  fv,
                                                ArrayList<Release> orderedReleases, Boolean extremeIncluded){
        int ivIndex = -1;
        int fvIndex = -1;
        int i=0;
        boolean contained = false;
        for (Release r: orderedReleases){
            if (iv.equalsIgnoreCase(r.name)) ivIndex=i;
            if (fv.equalsIgnoreCase(r.name)) fvIndex=i;
            if (currRelease.equalsIgnoreCase(r.name)){
                if(ivIndex == -1 && fvIndex == -1) {
                    contained = false;
                    break;
                }
                if(!extremeIncluded && ivIndex!=-1 && i>=ivIndex && fvIndex==-1){
                    contained=true;
                    break;
                }
                if(extremeIncluded && ivIndex!=-1 && i>=ivIndex && fvIndex!=-1 && i<=fvIndex){
                    contained=true;
                    break;
                }
            }
            i++;
        }
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
        return pinc;
    }

    private static void addMissingInjectedVersions(float p, ArrayList<Issue> allIssue){
        int ivIndex, ovIndex, fvIndex;

        for(Issue iss: allIssue){
            if(iss.injectedVersion == null) {

                fvIndex = iss.fixVersion.index;
                ovIndex = iss.openingVersion.index;
                ivIndex = Math.round((fvIndex-ovIndex)*p);
                iss.injectedVersion = allRelease.get(ivIndex);
            }
        }

    }




    public static void main(String[] args) throws IOException, JSONException, ParseException {

        String projName ="SYNCOPE";
        String projDirName = "C:\\Users\\Elisa Venditti\\Desktop\\syncope 1.2.10\\syncope";
//        String projName ="BOOKKEEPER";
//        String projDirName = "C:\\Users\\Elisa Venditti\\Desktop\\bookkeeper per bugseeker\\bookkeeper";

        allRelease = getAllRelease(projName);
        orderRelease();
        if(syncope){
            allRelease.add(new Release("3.0.0", longDate));
        }
        int z=0;
        for(Release r: allRelease){
            r.index=z;
            z++;
        }
        halfRelease = getHalfReleases(allRelease);
//        for(Release r: halfRelease)
//            System.out.println(r.name);
//        System.exit(200);


        ArrayList<Issue> allIssues  = getIssueIdOfProject(projName);


        ArrayList<Float> pinc = proportion(allIssues);
        int inc=0;
        for(Float p: pinc){
            System.out.println(inc+") "+pinc.get(inc));
            inc++;
        }
        int size=pinc.size();
        addMissingInjectedVersions(pinc.get(size-1), allIssues);


        ArrayList<Issue> valuableIssue = new ArrayList<>();
        int index;
        for(Issue i: allIssues){
            // delete issue with IV > halfRelease
            index=0;
            for(Release r: allRelease){
                if (r.name.equalsIgnoreCase(i.injectedVersion.name)){
                    break;
                }
                index++;
            }
            if(index<= halfRelease.size()-1) valuableIssue.add(i);
        }
        System.out.println("|valuableIssue|="+valuableIssue.size());
        System.exit(-200);









        Excel excel = new Excel(projDirName);
        FindCommits github = new FindCommits(projDirName + "\\.git", true);
//        FindCommits github = new FindCommits(projDirName + "\\.git", false);
//        github.getReleaseFileList(projName, excel, projDirName);
        ArrayList<Commit> commit_id = new ArrayList<>();
        int k=0;
        for(Issue i: valuableIssue){
            System.out.println("* search commit in issue "+k);
            List<Commit> list = github.getAllCommitsOfIssue(i, true);
//            List<Commit> list = github.getAllCommitsOfIssue(i, false);
            commit_id.addAll(list);
            k++;
        }
        System.out.println("|COMMIT|="+commit_id.size());


        for(Release release: halfRelease) {

            System.out.println("inizializzo excel per: "+release.name);
            for (Issue i : valuableIssue) {
                if(isReleaseContainedIn(release.name, i.injectedVersion.name, i.fixVersion.name, allRelease, false)) {
                    for(Commit com: commit_id){

                        if(isReleaseContainedIn(com.release.name, release.next().name,
                                i.fixVersion.name, allRelease, true)){
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
        allRelease=orderedRelease;
    }


}
