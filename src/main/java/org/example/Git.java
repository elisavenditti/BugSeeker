package org.example;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.example.Main.*;
import static org.example.MetricsCalculator.setLoc;
import static org.example.MetricsCalculator.setMetrics;


public class Git {
    private Repository repo;
    private String issueText;
    private String releaseAddingName;
    private String originalRelease;

    public Git(String gitPath, Boolean syncope){
        // gitPath
        try {
            FileRepositoryBuilder b = new FileRepositoryBuilder();
            this.repo = b.setGitDir(new File(gitPath)).build();
            if(syncope) releaseAddingName = "syncope";
            else releaseAddingName = "release";

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void checkoutTo(String version){
        try {
            org.eclipse.jgit.api.Git git = new org.eclipse.jgit.api.Git(repo);
            git.checkout().setName(version).call();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    public void resetRepo(){
        try {
            org.eclipse.jgit.api.Git git = new org.eclipse.jgit.api.Git(repo);
            git.checkout().setName(originalRelease).call();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }
    public void getReleaseFileList(String projName, Excel excel, String projDirName){
        int i=0;
        for(Release r: halfRelease) {
            System.out.println(i+") checkout to "+r.name+"++++++++++++++++++++++++++++++++++");
            checkoutTo(this.releaseAddingName + "-" + r.name);
            System.out.println("inizio a cercare i file");
            r.files = excel.listOfJavaFile(projDirName);
            System.out.println("ho finito di cercare i file");
            i++;
            setLoc(r);
        }


    }

    public List<Commit> getAllCommitsOfIssue(Issue issue, Boolean syncope){
        List<Commit> commitIds = new ArrayList<>();
        org.eclipse.jgit.api.Git git = new org.eclipse.jgit.api.Git(repo);
        if(syncope) this.issueText = "[" + issue.key + "]";
        else this.issueText = issue.key+":";
        try {
            Iterable<RevCommit> log = git.log().all().call();
            for(RevCommit i: log){
                String msg = i.getFullMessage();
                if(msg.contains(this.issueText)) {
                    PersonIdent authorIdent = i.getAuthorIdent();
                    String author = authorIdent.getName();
                    Date authorDate = authorIdent.getWhen();
                    Commit c = new Commit(i.getId().toString(), author, authorDate, issue);
                    String csha = c.getCommitSha();
                    try {
                        c.changedFiles.addAll(compareCommitWithPrevious(csha));
                    }catch (MyException e) {
                        System.out.println(e.getMessage());
                    }
                    commitIds.add(c);
                }
            }
            git.close();
            return commitIds;

        } catch (NoHeadException e) {
            throw new RuntimeException(e);
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private List<MyFile> getChangedFileWithLOC(String commitSha) throws MyException{

        List<MyFile> changedFiles = new ArrayList<>();
        org.eclipse.jgit.api.Git git = new org.eclipse.jgit.api.Git(repo);
        ObjectReader reader = git.getRepository().newObjectReader();
        CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
        ObjectId oldTree = null;
        try {
            oldTree = git.getRepository().resolve( commitSha+"~1^{tree}" );
            try{
                oldTreeIter.reset( reader, oldTree );
            }catch (Exception e){
                throw new MyException("Non esiste il commit precedente");
            }


            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            ObjectId newTree = git.getRepository().resolve( commitSha + "^{tree}" );
            newTreeIter.reset( reader, newTree );

            DiffFormatter diffFormatter = new DiffFormatter( DisabledOutputStream.INSTANCE );
            diffFormatter.setRepository( git.getRepository() );
            diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
            diffFormatter.setDetectRenames(true);

            int linesAdded ;
            int linesDeleted;
            int locTouched = 0; // aggiunte + modificate + cancellate

            List<DiffEntry> entries = diffFormatter.scan( oldTreeIter, newTreeIter );

            for( DiffEntry entry : entries ) {

                linesAdded = 0;
                linesDeleted = 0;
                for (Edit edit : diffFormatter.toFileHeader(entry).toEditList()) {

                    linesDeleted += edit.getEndA() - edit.getBeginA();
                    linesAdded += edit.getEndB() - edit.getBeginB();
                }

                locTouched = linesAdded + linesDeleted;
                String s;
                if(entry.getChangeType().equals(DiffEntry.ChangeType.DELETE))
                    s = entry.getOldPath().replaceAll("/","\\\\");
                else
                    s = entry.getNewPath().replaceAll("/","\\\\");

                MyFile newFile = new MyFile(s);
                newFile.locTouched = locTouched;
                newFile.locAdded = linesAdded;
                changedFiles.add(newFile);
            }
            return changedFiles;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    public List<MyFile> compareCommitWithPrevious(String commitSha) throws MyException{

        List<MyFile> changedFiles = new ArrayList<>();
        org.eclipse.jgit.api.Git git = new org.eclipse.jgit.api.Git(repo);
        ObjectReader reader = git.getRepository().newObjectReader();
        CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
        ObjectId oldTree = null;
        try {
            oldTree = git.getRepository().resolve( commitSha+"~1^{tree}" );
            try{
                oldTreeIter.reset( reader, oldTree );
            }catch (Exception e){
                throw new MyException("Non esiste il commit precedente");
            }

            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            ObjectId newTree = git.getRepository().resolve( commitSha + "^{tree}" );
            newTreeIter.reset( reader, newTree );

            DiffFormatter diffFormatter = new DiffFormatter( DisabledOutputStream.INSTANCE );
            diffFormatter.setRepository( git.getRepository() );
            List<DiffEntry> entries = diffFormatter.scan( oldTreeIter, newTreeIter );


            for( DiffEntry entry : entries ) {
                //String s = entry.getNewPath().replaceAll("/","\\\\");
                String s;
                if(entry.getChangeType().equals(DiffEntry.ChangeType.DELETE))
                    s = entry.getOldPath().replaceAll("/","\\\\");
                else
                    s = entry.getNewPath().replaceAll("/","\\\\");
                changedFiles.add(new MyFile(s));
            }
            return changedFiles;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void getMetrics() {

        ArrayList<Commit> commitIds = new ArrayList<>();
        org.eclipse.jgit.api.Git git = new org.eclipse.jgit.api.Git(repo);
        try {

            Iterable<RevCommit> log = git.log().all().call();
            for(RevCommit i: log){
                PersonIdent authorIdent = i.getAuthorIdent();
                String author = authorIdent.getName();
                Date authorDate = authorIdent.getWhen();
                Commit c = new Commit(i.getId().toString(), author, authorDate, null);
                commitIds.add(c);
                if(c.releaseIndex==-1 || c.releaseIndex > (halfRelease.size()-1))
                    continue;

                String csha = c.getCommitSha();

                try {
                    c.changedFiles.addAll(getChangedFileWithLOC(csha));
                } catch (MyException e){
                    System.out.println(e.getMessage());
                }



                setMetrics(c);

            }


            for (Release r: halfRelease) {
                if(r.index > (halfRelease.size()-1)) continue;
                System.out.println("Release: "+r.name);
                int k = 0;
                for (MyFile f : r.files){
                    if(k==0)System.out.println(f.pathname);

                    int avgLocAdded = 0;
                    int avgChgSetSize = 0;
                    int maxLocAdded = 0;
                    int maxChgSetSize = 0;
                    if(f.locAddedList.size()!=0) {
                        for (Integer add : f.locAddedList) {
                            if(add>maxLocAdded) maxLocAdded = add;
                            avgLocAdded = avgLocAdded + add;
                        }
                        avgLocAdded = avgLocAdded / (f.locAddedList.size());
                    }
                    if(f.chgSetSizeList.size()!=0) {
                        for (Integer add : f.chgSetSizeList) {
                            if(add>maxChgSetSize) maxChgSetSize = add;
                            avgChgSetSize = avgChgSetSize + add;
                        }
                        avgChgSetSize = avgChgSetSize / (f.chgSetSizeList.size());
                    }

                    System.out.println(" *file (" +k + ") - nRev:" + f.nRevisions+" nAuthors:"+f.getNumberOfAuthors() +
                            " nLocToccate:"+f.locTouched+ " nLocAggiunte:"+f.locAdded + " maxLocAggiunte:" + maxLocAdded + " avgLocAggiunte:"
                            + avgLocAdded + " chgSetSize:" + f.chgSetSize+ " avgChgSetSize:" + avgChgSetSize+ " maxChgSetSize:" + maxChgSetSize);
                    k++;
                }
            }
            git.close();
        } catch (NoHeadException e) {
            throw new RuntimeException(e);
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

}
