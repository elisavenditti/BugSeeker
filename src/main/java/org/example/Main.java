package org.example;

import org.example.core.Excel;
import org.example.core.Git;
import org.example.core.Jira;
import org.example.core.Proportion;
import org.example.entity.*;
import org.example.weka.Weka;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.example.core.Jira.*;

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
        // false for bookkeeper, true for syncope
        Config configuration = new Config(syncope);
        projName = configuration.getProjName();
        projDirName = configuration.getProjDirName();

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
            valuableIssue.addAll(getValuableIssue(allIssues, testing, trainingBoundary));

            ArrayList<Commit> commitId = new ArrayList<>();
            commitId.addAll(getCommits(valuableIssue, syncope, github));
            github.getMetrics();

            for (Release release : halfRelease) {

                for (Issue i : valuableIssue) {
                    if (isReleaseContainedIn(release, i.getInjectedVersion(), i.getFixVersion(), false)) {
                        release.addAllBuggyFiles(getFilesModifiedFromNowToFV(commitId, release, i));
                    }
                }
            }

            fillExcel(trainingBoundary, projName, excel);

        }

        Weka w = new Weka("C:\\Users\\Elisa Venditti\\Desktop\\ISW2\\BugSeeker\\");
        w.createArff(halfRelease.size(), syncope);
        w.walkforward(halfRelease.size(), syncope);

    }


    private static List<MyFile> getFilesModifiedFromNowToFV(List<Commit> commitId, Release release, Issue i) {
        List<MyFile> modifiedFiles = new ArrayList<>();
        for (Commit com : commitId) {

            if ((isReleaseContainedIn(com.getRelease(), release.next(), i.getFixVersion(), true)) &&
                    (com.getChangedFiles() != null && !com.getChangedFiles().isEmpty()))
                modifiedFiles.addAll(com.getChangedFiles());
        }
        return modifiedFiles;
    }


    private static void fillExcel(int trainingBoundary, String projName, Excel excel) {

        if (trainingBoundary != halfRelease.size() - 1)
            excel.populate(projName, trainingBoundary);
        else { //popolare gli excel di testing
            int testReleaseIndex;
            for (testReleaseIndex = 1; testReleaseIndex < halfRelease.size(); testReleaseIndex++) {
                try {
                    excel.populateTesting(projName, testReleaseIndex);
                } catch (IOException e) {
                    Logger logger = Logger.getLogger(Main.class.getName());
                    logger.log(Level.INFO, e.getMessage());
                }
            }
        }
    }


    private static List<Issue> getValuableIssue(List<Issue> allIssues, boolean testing, int trainingBoundary) {
        ArrayList<Issue> valuableIssue = new ArrayList<>();
        for (Issue i : allIssues) {
            // for testing set delete issue with IV greater than releases in the first half
            if (testing && i.getInjectedVersion().getIndex() <= halfRelease.size() - 1) valuableIssue.add(i);

            // for training set delete issue with FV > trainingBoundary (I CAN'T SEE FUTURE INFORMATION)
            if (!testing && i.getFixVersion().getIndex() <= trainingBoundary) valuableIssue.add(i);

        }
        return valuableIssue;
    }

    private static List<Commit> getCommits(List<Issue> valuableIssue, boolean syncope, Git github) {
        List<Commit> commitId = new ArrayList<>();
        for (Issue i : valuableIssue) {
            List<Commit> list = github.getAllCommitsOfIssue(i, syncope);
            commitId.addAll(list);
        }
        return commitId;
    }


    public static void deleteDuplicate() {
        ArrayList<Release> uniqueReleaseList = new ArrayList<>();
        int thisIndex;
        int nextIndex;
        int lastIndex;
        lastIndex = allRelease.size() - 1;
        for (Release r : allRelease) {
            thisIndex = r.getIndex();
            nextIndex = thisIndex + 1;
            if (thisIndex == lastIndex ||
                    !allRelease.get(thisIndex).getReleaseDate().equals(allRelease.get(nextIndex).getReleaseDate()))
                uniqueReleaseList.add(allRelease.get(thisIndex));

        }
        allRelease = indexOrderedReleases(uniqueReleaseList);
    }

    public static List<Release> indexOrderedReleases(List<Release> releaseList) {
        int z = 0;
        for (Release r : releaseList) {
            r.setIndex(z);
            z++;
        }
        return releaseList;
    }

    public static void orderRelease() {
        ArrayList<Release> orderedRelease = new ArrayList<>();
        for (Release rr : allRelease) {
            if (orderedRelease.isEmpty())
                orderedRelease.add(rr);
            else {
                int count = 0;
                while (count < orderedRelease.size()) {
                    if ((orderedRelease.get(count).getReleaseDate()).after(rr.getReleaseDate())) break;
                    count++;
                }

                if (count == orderedRelease.size())
                    orderedRelease.add(rr);
                else
                    orderedRelease.add(count, rr);

            }

        }
        allRelease = indexOrderedReleases(orderedRelease);
    }

    private static boolean isReleaseContainedIn(Release currRelease, Release iv, Release fv, boolean extremeIncluded) {
        int ivIndex = iv.getIndex();
        int fvIndex = fv.getIndex();
        int i = currRelease.getIndex();
        boolean contained = false;

        if ((extremeIncluded && i >= ivIndex && i <= fvIndex) || (!extremeIncluded && i >= ivIndex && i < fvIndex))
            contained = true;
        return contained;
    }


}
