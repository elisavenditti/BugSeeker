package org.example;

import org.json.JSONException;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

import static java.lang.Math.floor;
import static org.example.Jira.*;

public class Main {

    public static ArrayList<Release> allRelease;
    public static ArrayList<Release> halfRelease;
    public static int rootLen;



//    public static void main(String[] args) throws IOException, JSONException, ParseException {
//
////        String projName ="SYNCOPE";
////        String projDirName = "C:\\Users\\Elisa Venditti\\Desktop\\syncope 1.2.10\\syncope";
//
//        String projName ="BOOKKEEPER";
//        String projDirName = "C:\\Users\\Elisa Venditti\\Desktop\\bookkeeper per bugseeker\\bookkeeper";
//        Boolean syncope = false;
//        Excel excel = new Excel(projDirName);
//        String rootDir = projDirName+"\\";
//        rootLen = rootDir.length();
//
//
//        allRelease = getAllRelease(projName);
//        orderRelease();
//        deleteDuplicate();
//        halfRelease = getHalfReleases(allRelease);
//
////        for(Release r: halfRelease)
////            System.out.println(r.index+") "+r.name + " ("+r.releaseDate.toString()+")");
//
//
//
//        ArrayList<Issue> allIssues  = getIssueIdOfProject(projName);
//
//        Proportion proportion = new Proportion();
//        ArrayList<Float> pinc = proportion.incremental(allIssues, true);
//
//
//        allIssues = proportion.addMissingInjectedVersions(pinc, allIssues);
//
//        ArrayList<Issue> valuableIssue = new ArrayList<>();
//        for(Issue i: allIssues){
//            // delete issue with IV > halfRelease
//            if(i.injectedVersion.index<=halfRelease.size()-1) valuableIssue.add(i);
//
//        }
//        System.out.println("|valuableIssue|="+valuableIssue.size());
//
//        Git github = new Git(projDirName + "\\.git", syncope);
//        github.getReleaseFileList(projName, excel, projDirName);
//
//        ArrayList<Commit> commit_id = new ArrayList<>();
//        int k=0;
//        for(Issue i: valuableIssue){
//            System.out.println("* search commit in issue "+k);
//            List<Commit> list = github.getAllCommitsOfIssue(i, syncope);
//            commit_id.addAll(list);
//            k++;
//
//        }
//        System.out.println("|COMMIT|="+commit_id.size());
//        github.getMetrics();
//
//        for(Release release: halfRelease) {
//
//            System.out.println("inizializzo excel per: "+release.name);
//            for (Issue i : valuableIssue) {
//                if(isReleaseContainedIn(release, i.injectedVersion, i.fixVersion, false)) {
//                    for(Commit com: commit_id){
//
//                        if(isReleaseContainedIn(com.release, release.next(), i.fixVersion, true)){
//                            if(com.changedFiles!=null && com.changedFiles.size()>0)
//                                release.buggyFiles.addAll(com.changedFiles);
//                        }
//
//
//                    }
//                }
//            }
//        }
//
//
//        excel.populate(projName);
//
//    }




    public static void main(String[] args) throws IOException, JSONException, ParseException {

        String projName, projDirName;
        Boolean syncope = true;

        if(syncope){
            projName ="SYNCOPE";
            projDirName = "C:\\Users\\Elisa Venditti\\Desktop\\syncope 1.2.10\\syncope";
        }else{
            projName ="BOOKKEEPER";
            projDirName = "C:\\Users\\Elisa Venditti\\Desktop\\bookkeeper per bugseeker\\bookkeeper";
        }

        Excel excel = new Excel(projDirName);
        String rootDir = projDirName + "\\";
        rootLen = rootDir.length();


        allRelease = getAllRelease(projName);
        orderRelease();
        deleteDuplicate();
        halfRelease = getHalfReleases(allRelease);
        ArrayList<Issue> allIssues  = getIssueIdOfProject(projName);

        Proportion proportion = new Proportion();
        ArrayList<Float> pinc = proportion.incremental(allIssues, true);
        allIssues = proportion.addMissingInjectedVersions(pinc, allIssues, true);


        Git github = new Git(projDirName + "\\.git", syncope);
        github.getReleaseFileList(excel, projDirName);


        int trainingBoundary;
        boolean testing = false;
        ArrayList<Release> toRestoreAllRelease = allRelease;
        ArrayList<Release> toRestoreHalfRelease = halfRelease;

        for(trainingBoundary=0; trainingBoundary< halfRelease.size(); trainingBoundary++) {
            allRelease=toRestoreAllRelease;
            halfRelease=toRestoreHalfRelease;
            // the last loop's iteration is dedicated to TESTING
            if(trainingBoundary == halfRelease.size() - 1){
                testing = true;
                // testing set must be labeled with the whole information
                ArrayList<Float> p = new ArrayList<>();
                p.add(proportion.globalP(allIssues));
                allIssues = proportion.addMissingInjectedVersions(p, allIssues, false);
            }

            ArrayList<Issue> valuableIssue = new ArrayList<>();
            for (Issue i : allIssues) {
                // for testing set delete issue with IV greater than releases in the first half
                if(testing && i.injectedVersion.index<=halfRelease.size()-1) valuableIssue.add(i);

                // for training set delete issue with FV > trainingBoundary (I CAN'T SEE FUTURE INFORMATION)
                if (!testing && i.fixVersion.index <= trainingBoundary) valuableIssue.add(i);

            }

            ArrayList<Commit> commitId = new ArrayList<>();
            for (Issue i : valuableIssue) {
                ArrayList<Commit> list = github.getAllCommitsOfIssue(i, syncope);
                commitId.addAll(list);
            }
            github.getMetrics();

            for (Release release : halfRelease) {

                for (Issue i : valuableIssue) {
                    if (isReleaseContainedIn(release, i.injectedVersion, i.fixVersion, false)) {
                        for (Commit com : commitId) {

                            if (isReleaseContainedIn(com.release, release.next(), i.fixVersion, true)) {
                                if (com.changedFiles != null && com.changedFiles.size() > 0)
                                    release.buggyFiles.addAll(com.changedFiles);
                            }


                        }
                    }
                }
            }

            if(!(trainingBoundary==halfRelease.size() -1))
                excel.populate(projName, trainingBoundary);
            else { //popolare gli excel di testing
                int testReleaseIndex;
                for(testReleaseIndex=1; testReleaseIndex< halfRelease.size(); testReleaseIndex++)
                    excel.populateTesting(projName, testReleaseIndex);
            }
        }

        Weka w = new Weka();
        w.createArff(halfRelease.size(), syncope);
        w.walkforward(halfRelease.size(), syncope);


    }




    public static void deleteDuplicate() {
        ArrayList<Release> uniqueReleaseList = new ArrayList<>();
        int thisIndex, nextIndex, lastIndex;
        lastIndex = allRelease.size()-1;
        for(Release r: allRelease){
            thisIndex = r.index;
            nextIndex = thisIndex+1;
            if(thisIndex == lastIndex ||
                    !allRelease.get(thisIndex).releaseDate.equals(allRelease.get(nextIndex).releaseDate))
                uniqueReleaseList.add(allRelease.get(thisIndex));

        }
        int z=0;
        for(Release r: uniqueReleaseList){
            r.index=z;
            z++;
        }
        allRelease = uniqueReleaseList;
    }
    public static void orderRelease() {
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

    private static boolean isReleaseContainedIn(Release currRelease, Release iv, Release  fv, Boolean extremeIncluded){
        int ivIndex = iv.index;
        int fvIndex = fv.index;
        int i= currRelease.index;
        boolean contained = false;

        if(extremeIncluded && i>=ivIndex && i<=fvIndex) contained = true;
        else if(!extremeIncluded && i>=ivIndex && i<fvIndex) contained = true;
        return contained;
    }


}
