package org.example;
import org.eclipse.jgit.api.errors.GitAPIException;
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

    public Git(String gitPath, Boolean syncope){

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

    public void getReleaseFileList(Excel excel, String projDirName){
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

    public ArrayList<Commit> getAllCommitsOfIssue(Issue issue, Boolean syncope){
        ArrayList<Commit> commitIds = new ArrayList<>();
        org.eclipse.jgit.api.Git git = new org.eclipse.jgit.api.Git(repo);

        if(syncope)
            this.issueText = "[" + issue.key + "]";
        else
            this.issueText = issue.key+":";

        try {
            Iterable<RevCommit> log = git.log().all().call();
            for(RevCommit i: log){
                String msg = i.getFullMessage();
                if(msg.contains(this.issueText)) {
                    PersonIdent authorIdentity = i.getAuthorIdent();
                    String author = authorIdentity.getName();
                    Date authorDate = authorIdentity.getWhen();
                    Commit c = new Commit(i.getId().toString(), author, authorDate);
                    String cSha = c.getCommitSha();
                    try {
                        c.changedFiles.addAll(compareCommitWithPrevious(cSha));
                    }catch (MyException e) {
                        System.out.println(e.getMessage());
                    }
                    if(c.release!=null)
                        commitIds.add(c);
                }
            }
            git.close();
            return commitIds;

        } catch (GitAPIException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    private List<MyFile> getChangedFileWithLOC(String commitSha) throws MyException{

        List<MyFile> changedFiles = new ArrayList<>();
        org.eclipse.jgit.api.Git git = new org.eclipse.jgit.api.Git(repo);
        ObjectReader reader = git.getRepository().newObjectReader();
        CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
        ObjectId oldTree;
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
            int locTouched; // added + modified + deleted

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
        ObjectId oldTree;
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
                if(entry.getChangeType()==DiffEntry.ChangeType.DELETE||
                        entry.getChangeType()==DiffEntry.ChangeType.MODIFY){
                    String s;
//                    if(entry.getChangeType().equals(DiffEntry.ChangeType.DELETE))
//                        s = entry.getOldPath().replaceAll("/","\\\\");
//                    else
                        s = entry.getNewPath().replaceAll("/","\\\\");
                    changedFiles.add(new MyFile(s));
                }
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
                PersonIdent authorIdentity = i.getAuthorIdent();
                Commit c = new Commit(i.getId().toString(), authorIdentity.getName(), authorIdentity.getWhen());
                commitIds.add(c);
                if(c.releaseIndex==-1 || c.releaseIndex > (halfRelease.size()-1))
                    continue;

                String cSha = c.getCommitSha();

                try {
                    c.changedFiles.addAll(getChangedFileWithLOC(cSha));
                } catch (MyException e){
                    System.out.println(e.getMessage());
                }

                setMetrics(c);

            }
            git.close();
        } catch (GitAPIException | IOException e) {
            throw new RuntimeException(e);
        }


    }

}
