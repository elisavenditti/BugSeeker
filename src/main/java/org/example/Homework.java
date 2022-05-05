//package org.example;
//import org.json.JSONException;
//
//import java.io.IOException;
//import java.text.DateFormat;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//import java.util.Map;
//import static org.example.Jira.*;
//
//public class Homework {
//    public static ArrayList<Release> allRelease;
//    private static String getCommitSha(String commitId){
//
//        String[] parts = commitId.split(" ");
//        return parts[1];
//    }
//
//    private static void homework(String projName, String affectedVersion, String projDirName) throws IOException {
//        // da chiamare così:
//        // String affectedVersion ="1.2.10";
//        // homework(projName,affectedVersion,projDirName);
//        List<String> changedFiles = new ArrayList<>();
//        List<String> commits = new ArrayList<>();
//
//        // 1. data una versione (release) trovo gli id di tutti gli issue che l'hanno influenzata
//        Map<String, List<String>> issuesId  = getIssueIdOfVersion(projName, affectedVersion);
//
//
//        FindCommits findCommits = new FindCommits(projDirName + "\\.git","syncope-1.2.10");
//        Boolean checkout;
//
//        // 2. vado nelle varie fixVersion e cerco i commit che nominano gli issue trovati
//        for(String fixVer: issuesId.get("fixVersions")){
//            checkout=true;
//            System.out.println("************************CHECKOUT TO "+fixVer+"************************");
//            for(String id: issuesId.get("issues")){
//                List<String> commit_id = findCommits.findCommitWithSpecificText(id,
//                        projName.toLowerCase()+"-"+fixVer, checkout);
//                checkout = false;
//
//                // 3. dal commit mi ritrovo le classi coinvolte (nella release considerata sono buggy!)
//                for (String cid: commit_id){
//                    String csha = getCommitSha(cid);
//                    commits.add(csha);
//                    changedFiles.addAll(findCommits.compareCommitWithPrevious(csha));
//                }
//            }
//        }
//        findCommits.resetRepo();
//        System.out.println("++++++++++++++++++++++CHANGED FILES++++++++++++++++++++++++++");
//        for(String s: changedFiles)System.out.println(s);
//
//        System.out.println("++++++++++++++++++++++INIZIO L'EXCEL++++++++++++++++++++++++++");
//
//        //4. costruisco la base per il file excel
//
//        Excel excel = new Excel(projDirName);
//        List<String> fileList= excel.listOfJavaFile(projDirName);
//        String excelName = excel.createExcelFile("syncope", fileList);
//
//
//        System.out.println("++++++++++++++++++++++MODIFICO L'EXCEL++++++++++++++++++++++++++");
//
//        //5. Modifico l'excel indicando come buggy le classi toccate dai commit
//        excel.modifyAllVerticalCell(2, excelName, changedFiles);
//
//
//    }
//
//
//
//
//    private static boolean isReleaseContainedIn(String currRelease, String iv, String  fv, ArrayList<Release> orderedReleases){
//        int ivIndex = -1;
//        int fvIndex = -1;
//        int i=0;
//        boolean contained = false;
//        for (Release r: orderedReleases){
//            if (iv.equalsIgnoreCase(r.name)) ivIndex=i;
//            if (fv.equalsIgnoreCase(r.name)) fvIndex=i;
//            if (currRelease.equalsIgnoreCase(r.name)){
//                if(ivIndex == -1 && fvIndex == -1) {
//                    contained = false;
//                    break;
//                }
//                if(ivIndex!=-1 && i>=ivIndex && fvIndex==-1){
//                    contained=true;
//                    break;
//                }
//            }
//            i++;
//        }
//        return contained;
//    }
//
//
//
//    public static void main(String[] args) throws IOException, JSONException, ParseException {
//
//        String projName ="SYNCOPE";
//        String projDirName = "C:\\Users\\Elisa Venditti\\Desktop\\syncope 1.2.10\\syncope";
//
//        allRelease = getAllRelease(projName);
//        ArrayList<Release> halfRelease = getHalfReleases(allRelease);
//
////        for(Release el: halfRelease)
////            System.out.println(el.name+":"+el.releaseDate);
//
//        //2. trova con jira tutti i bug risolti nel progetto
//        ArrayList<Issue> allIssues  = getIssueIdOfProject(projName);
//
//        for(Release release: halfRelease) {
//
//            String currRelease = release.name;
//            Date currReleaseDate = release.releaseDate;
//
//
//            //checkout su questa release
//            System.out.println("********************Checkout to " + currRelease + "********************");
//            FindCommits github = new FindCommits(projDirName + "\\.git");
//            github.checkoutTo(projName.toLowerCase() + "-" + currRelease);
//
//            //popolo l'excel con le classi della release
//
//            Excel excel = new Excel(projDirName);
//            List<String> fileList = excel.listOfJavaFile(projDirName);
//            String excelName = excel.addClasses("syncope", fileList, currRelease);
//
//
//            for (Issue i : allIssues) {
//
//
//                if(isReleaseContainedIn(currRelease, i.injectedVersion, i.uniqueFixVersion, allRelease)) {
//
//                    List<String> commit_id = github.findCommitWithSpecificText(i.key, null, false);
////                    if (currReleaseDate.before(i.fixDate) /*&& currReleaseDate.after(i.injectedVersion)*/
////                            && !(i.uniqueFixVersion.equalsIgnoreCase(currRelease))) {
//
//                        //cerca i file modificati dai commit e modifica l'excel dicendo che erano buggy
//                    List<String> changedFiles = new ArrayList<>();
//                    for (String cid : commit_id) {
//                        String csha = getCommitSha(cid);
//                        changedFiles.addAll(github.compareCommitWithPrevious(csha));
//                    }
//                    excel.modifyBuggyCellOfRelease(currRelease, 2, excelName, changedFiles);
//
////                    } else {
////                        System.out.println("[ERRORE] - Release(" + currRelease + ": " + currReleaseDate + ") " +
////                                "; IssueFixDate:" + i.fixDate.toString());
////                        System.out.print("    fix releases:");
////                        for (String k : i.jiraFixVersions) System.out.print(k);
////                        System.out.println("    affected releases:");
////                        for (String k : i.affectedVersions) System.out.print(k);
////
////                        if (i.affectedVersions.contains(currRelease))
////                            System.out.println(".................... salto l'issue perchè AV=FV");
////                        else if (currReleaseDate.after(i.fixDate)) {
////                            System.out.println(".................... ho un commit con un issue dopo la sua FIX VERSION");
////                        } else {
////                            System.out.println(".................... la fix date non è uguale alla resolutiondate su jira: salto l'issue");
////                        }
////                    }
//
//                }
//            }
//        }
//    }
//
//}
