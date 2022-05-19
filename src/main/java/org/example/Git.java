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
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.example.Main.*;
import static org.example.MetricsCalculator.setLoc;
import static org.example.MetricsCalculator.setMetrics;


public class Git {
    private Repository repo;
    private String releaseAddingName;

    public Git(String gitPath, boolean syncope){

        try {
            FileRepositoryBuilder b = new FileRepositoryBuilder();
            this.repo = b.setGitDir(new File(gitPath)).build();
            if(syncope) releaseAddingName = "syncope";
            else releaseAddingName = "release";

        } catch (IOException e) {
            Logger logger = Logger.getLogger(Issue.class.getName());
            logger.log(Level.INFO, e.getMessage());
        }
    }


    public void checkoutTo(String version){
        try {
            org.eclipse.jgit.api.Git git = new org.eclipse.jgit.api.Git(repo);
            git.checkout().setName(version).call();
        } catch (GitAPIException e) {
            Logger logger = Logger.getLogger(Issue.class.getName());
            logger.log(Level.INFO, e.getMessage());
        }
    }

    public void getReleaseFileList(Excel excel, String projDirName){
        int i=0;
        for(Release r: Main.getHalfRelease()) {
            Logger logger = Logger.getLogger(Issue.class.getName());
            logger.log(Level.INFO, i+") checkout to "+r.name+"++++++++++++++++++++++++++++++++++");

            checkoutTo(this.releaseAddingName + "-" + r.name);
            logger.log(Level.INFO, "inizio a cercare i file");
            r.files = excel.listOfJavaFile(projDirName);
            logger.log(Level.INFO, "ho finito di cercare i file");
            i++;
            setLoc(r);
        }


    }

    public List<Commit> getAllCommitsOfIssue(Issue issue, boolean syncope){
        String issueText;
        ArrayList<Commit> commitIds = new ArrayList<>();
        org.eclipse.jgit.api.Git git = new org.eclipse.jgit.api.Git(repo);

        if(syncope)
            issueText = "[" + issue.getKey() + "]";
        else
            issueText = issue.getKey()+":";

        try {
            Iterable<RevCommit> log = git.log().all().call();
            for(RevCommit i: log){
                String msg = i.getFullMessage();
                if(msg.contains(issueText)) {
                    PersonIdent authorIdentity = i.getAuthorIdent();
                    String author = authorIdentity.getName();
                    Date authorDate = authorIdentity.getWhen();
                    Commit c = new Commit(i.getId().toString(), author, authorDate);
                    String cSha = c.getCommitSha();


                    c.getChangedFiles().addAll(compareCommitWithPrevious(cSha));

                    if(c.getRelease()!=null)
                        commitIds.add(c);
                }
            }
            git.close();
            return commitIds;

        } catch (GitAPIException | IOException e) {
            Logger logger = Logger.getLogger(Issue.class.getName());
            logger.log(Level.INFO, e.getMessage());
        }
        return commitIds;
    }


    private List<MyFile> getChangedFileWithLOC(String commitSha){

        List<MyFile> changedFiles = new ArrayList<>();
        org.eclipse.jgit.api.Git git = new org.eclipse.jgit.api.Git(repo);
        ObjectReader reader = git.getRepository().newObjectReader();
        CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
        ObjectId oldTree;
        try {
            oldTree = git.getRepository().resolve( commitSha+"~1^{tree}" );
            oldTreeIter.reset( reader, oldTree );



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
                    s = entry.getOldPath().replace("/","\\\\");
                else
                    s = entry.getNewPath().replace("/","\\\\");

                MyFile newFile = new MyFile(s);
                newFile.setLocAdded(locTouched);
                newFile.setLocAdded(linesAdded);
                changedFiles.add(newFile);
            }
            return changedFiles;
        } catch (IOException e) {
            Logger logger = Logger.getLogger(Issue.class.getName());
            logger.log(Level.INFO, e.getMessage());
        }
        return changedFiles;
    }



    public List<MyFile> compareCommitWithPrevious(String commitSha){

        List<MyFile> changedFiles = new ArrayList<>();
        org.eclipse.jgit.api.Git git = new org.eclipse.jgit.api.Git(repo);
        ObjectReader reader = git.getRepository().newObjectReader();
        CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
        ObjectId oldTree;
        try {
            oldTree = git.getRepository().resolve( commitSha+"~1^{tree}" );
            oldTreeIter.reset( reader, oldTree );


            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            ObjectId newTree = git.getRepository().resolve( commitSha + "^{tree}" );
            newTreeIter.reset( reader, newTree );

            DiffFormatter diffFormatter = new DiffFormatter( DisabledOutputStream.INSTANCE );
            diffFormatter.setRepository( git.getRepository() );
            List<DiffEntry> entries = diffFormatter.scan( oldTreeIter, newTreeIter );


            for( DiffEntry entry : entries ) {
                if(entry.getChangeType()==DiffEntry.ChangeType.DELETE||
                        entry.getChangeType()==DiffEntry.ChangeType.MODIFY){
                    String s = entry.getNewPath().replace("/","\\\\");
                    changedFiles.add(new MyFile(s));
                }
            }
            return changedFiles;
        } catch (IOException e) {
            Logger logger = Logger.getLogger(Issue.class.getName());
            logger.log(Level.INFO, e.getMessage());
        }
        return changedFiles;
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
                if(c.getReleaseIndex()==-1 || c.getReleaseIndex() > (Main.getHalfRelease().size()-1))
                    continue;

                String cSha = c.getCommitSha();
                c.getChangedFiles().addAll(getChangedFileWithLOC(cSha));
                setMetrics(c);

            }
            git.close();
        } catch (GitAPIException | IOException e) {
            Logger logger = Logger.getLogger(Issue.class.getName());
            logger.log(Level.INFO, e.getMessage());
        }


    }

}
