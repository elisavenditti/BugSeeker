package org.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static org.example.Jira.*;

public class Main {

    private static List<Release> allRelease;
    private static List<Release> halfRelease;
    private static int rootLen;

    public static List<Release> getAllRelease() {
        return allRelease;
    }

    public static void setAllRelease(List<Release> allRelease) {
        Main.allRelease = allRelease;
    }

    public static List<Release> getHalfRelease() {
        return halfRelease;
    }

    public static void setHalfRelease(List<Release> halfRelease) {
        Main.halfRelease = halfRelease;
    }

    public static int getRootLen() {
        return rootLen;
    }


    public static void main(String[] args) throws IOException {

        String projName;
        String projDirName;
        boolean syncope = false;

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


        allRelease = Jira.getAllRelease(projName);
        orderRelease();
        deleteDuplicate();
        halfRelease = getHalfReleases(allRelease);
        List<Issue> allIssues  = getIssueIdOfProject(projName);

        Proportion proportion = new Proportion();
        List<Float> pinc = proportion.incremental(allIssues, true);
        allIssues = proportion.addMissingInjectedVersions(pinc, allIssues, true);


        Git github = new Git(projDirName + "\\.git", syncope);
        github.getReleaseFileList(excel, projDirName);


        int trainingBoundary;
        boolean testing = false;
        List<Release> toRestoreAllRelease = allRelease;
        List<Release> toRestoreHalfRelease = halfRelease;

        for(trainingBoundary=0; trainingBoundary< halfRelease.size(); trainingBoundary++) {
            allRelease=toRestoreAllRelease;
            halfRelease=toRestoreHalfRelease;
            // the last loop's iteration is dedicated to TESTING
            if(trainingBoundary == halfRelease.size() - 1){
                testing = true;
                // p is calculated using the last entry of pinc (so using ALL issues of the project)
                allIssues = proportion.addMissingInjectedVersions(pinc, allIssues, false);
            }

            ArrayList<Issue> valuableIssue = new ArrayList<>();
            for (Issue i : allIssues) {
                // for testing set delete issue with IV greater than releases in the first half
                if(testing && i.getInjectedVersion().getIndex()<=halfRelease.size()-1) valuableIssue.add(i);

                // for training set delete issue with FV > trainingBoundary (I CAN'T SEE FUTURE INFORMATION)
                if (!testing && i.getFixVersion().getIndex() <= trainingBoundary) valuableIssue.add(i);

            }

            ArrayList<Commit> commitId = new ArrayList<>();
            for (Issue i : valuableIssue) {
                List<Commit> list = github.getAllCommitsOfIssue(i, syncope);
                commitId.addAll(list);
            }
            github.getMetrics();

            for (Release release : halfRelease) {

                for (Issue i : valuableIssue) {
                    if (isReleaseContainedIn(release, i.getInjectedVersion(), i.getFixVersion(), false)) {
                        for (Commit com : commitId) {

                            if ((isReleaseContainedIn(com.getRelease(), release.next(), i.getFixVersion(), true)) &&
                                    (com.getChangedFiles() != null && !com.getChangedFiles().isEmpty()))
                                release.addAllBuggyFiles(com.getChangedFiles());

                        }
                    }
                }
            }

            if(trainingBoundary!=halfRelease.size() -1)
                excel.populate(projName, trainingBoundary);
            else { //popolare gli excel di testing
                int testReleaseIndex;
                for(testReleaseIndex=1; testReleaseIndex< halfRelease.size(); testReleaseIndex++)
                    excel.populateTesting(projName, testReleaseIndex);
            }
        }

        Weka w = new Weka("C:\\Users\\Elisa Venditti\\Desktop\\ISW2\\BugSeeker\\");
        w.createArff(halfRelease.size(), syncope);
        w.walkforward(halfRelease.size(), syncope);


    }




    public static void deleteDuplicate() {
        ArrayList<Release> uniqueReleaseList = new ArrayList<>();
        int thisIndex;
        int nextIndex;
        int lastIndex;
        lastIndex = allRelease.size()-1;
        for(Release r: allRelease){
            thisIndex = r.getIndex();
            nextIndex = thisIndex+1;
            if(thisIndex == lastIndex ||
                    !allRelease.get(thisIndex).getReleaseDate().equals(allRelease.get(nextIndex).getReleaseDate()))
                uniqueReleaseList.add(allRelease.get(thisIndex));

        }
        allRelease = indexOrderedReleases(uniqueReleaseList);
    }

    public static List<Release> indexOrderedReleases(List<Release> releaseList){
        int z=0;
        for(Release r: releaseList){
            r.setIndex(z);
            z++;
        }
        return releaseList;
    }
    public static void orderRelease() {
        ArrayList<Release> orderedRelease = new ArrayList<>();
        for(Release rr: allRelease){
            if (orderedRelease.isEmpty())
                orderedRelease.add(rr);
            else{
                int count=0;
                while(count< orderedRelease.size()){
                    if((orderedRelease.get(count).getReleaseDate()).after(rr.getReleaseDate())) break;
                    count++;
                }

                if(count==orderedRelease.size())
                    orderedRelease.add(rr);
                else
                    orderedRelease.add(count,rr);

            }

        }
        allRelease = indexOrderedReleases(orderedRelease);
    }

    private static boolean isReleaseContainedIn(Release currRelease, Release iv, Release  fv, boolean extremeIncluded){
        int ivIndex = iv.getIndex();
        int fvIndex = fv.getIndex();
        int i= currRelease.getIndex();
        boolean contained = false;

        if((extremeIncluded && i>=ivIndex && i<=fvIndex) || (!extremeIncluded && i>=ivIndex && i<fvIndex))
            contained = true;
        return contained;
    }


}
