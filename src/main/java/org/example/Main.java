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



    public static void main(String[] args) throws IOException, JSONException, ParseException {

        String projName ="SYNCOPE";
        String projDirName = "C:\\Users\\Elisa Venditti\\Desktop\\syncope 1.2.10\\syncope";

        allRelease = getAllRelease(projName);
        halfRelease = getHalfReleases(allRelease);
        ArrayList<Issue> allIssues  = getIssueIdOfProject(projName);
        ArrayList<Issue> valuableIssue = new ArrayList<>();
        int index=0;
        for(Issue i: allIssues){
            // delete issue with IV > halfRelease
            index=0;
            for(Release r: allRelease){
                if (r.name.equalsIgnoreCase(i.injectedVersion)){
                    break;
                }
                index++;
            }
            if(index<= halfRelease.size()-1) valuableIssue.add(i);
        }
        System.out.println("|valuableIssue|="+valuableIssue.size());


        Excel excel = new Excel(projDirName);
        FindCommits github = new FindCommits(projDirName + "\\.git");
        github.getReleaseFileList(projName, excel, projDirName);
        ArrayList<Commit> commit_id = new ArrayList<>();
        int k=0;
        for(Issue i: valuableIssue){
            System.out.println("* search commit in issue "+k);
            List<Commit> list = github.getAllCommitsOfIssue(i);

            commit_id.addAll(list);
            k++;
        }

        for(Release release: halfRelease) {

            System.out.println("inizializzo excel per: "+release.name);
            for (Issue i : allIssues) {
                if(isReleaseContainedIn(release.name, i.injectedVersion, i.uniqueFixVersion, allRelease, false)) {
                    for(Commit com: commit_id){

                        if(isReleaseContainedIn(com.release.name, release.next().name,
                                i.uniqueFixVersion, allRelease, true)){
                            if(com.changedFiles!=null && com.changedFiles.size()>0)
                                release.buggyFiles.addAll(com.changedFiles);
                        }


                    }
                }
            }
        }

        excel.populate(projName);

    }





}
