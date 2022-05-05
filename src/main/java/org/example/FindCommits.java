package org.example;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.example.Main.halfRelease;


public class FindCommits {
    private Repository repo;
    private String originalRelease;

    public FindCommits(String gitPath, String originalTag){
        // gitPath
        try {
            FileRepositoryBuilder b = new FileRepositoryBuilder();
            this.repo = b.setGitDir(new File(gitPath)).build();
            this.originalRelease = originalTag;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public FindCommits(String gitPath){
        // gitPath
        try {
            FileRepositoryBuilder b = new FileRepositoryBuilder();
            this.repo = b.setGitDir(new File(gitPath)).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void checkoutTo(String version){
        try {
            Git git = new Git(repo);
            git.checkout().setName(version).call();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }



    public void resetRepo(){
        try {
            Git git = new Git(repo);
            git.checkout().setName(originalRelease).call();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }
    public List<String> findCommitWithSpecificText(String text, String checkoutVersion, Boolean checkout){
        List<String> commitId = new ArrayList<>();
        Git git = new Git(repo);
        try {
            if (checkout){
                try {

                    git.checkout().setName(checkoutVersion).call();
                } catch (RefNotFoundException e) {
                    git.close();
                    return new ArrayList<>();
                }
            }


            Iterable<RevCommit> log = git.log().call();
            for(RevCommit i: log){
                String msg = i.getFullMessage();
                if(msg.contains(text)){

                    System.out.println("Found Commit: " + i);
                    System.out.println("--> " + msg);
                    commitId.add(i.getId().toString());
                }

            }
            git.close();
            return commitId;

        } catch (NoHeadException e) {
            throw new RuntimeException(e);
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    public void getReleaseFileList(String projName, Excel excel, String projDirName){
        int i=0;
        for(Release r: halfRelease) {
            System.out.println(i+") checkout to "+r.name+"++++++++++++++++++++++++++++++++++");
            checkoutTo(projName.toLowerCase() + "-" + r.name);
            r.files = (ArrayList<String>) excel.listOfJavaFile(projDirName);
            i++;
        }
    }

    public List<Commit> getAllCommitsOfIssue(Issue issue){
        List<Commit> commitIds = new ArrayList<>();
        Git git = new Git(repo);
        try {
            Iterable<RevCommit> log = git.log().all().call();
            for(RevCommit i: log){
                String msg = i.getFullMessage();
                if(msg.contains(issue.key)){

                    PersonIdent authorIdent = i.getAuthorIdent();
                    Date authorDate = authorIdent.getWhen();

                    Commit c = new Commit(i.getId().toString(), authorDate, issue);
                    String csha = c.getCommitSha();
                    c.changedFiles.addAll(compareCommitWithPrevious(csha));
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




    public List<String> compareCommitWithPrevious(String commitSha){

        List<String> changedFiles = new ArrayList<>();
        Git git = new Git(repo);
        ObjectReader reader = git.getRepository().newObjectReader();
        CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
        ObjectId oldTree = null;
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
                String s = entry.getNewPath().replaceAll("/","\\\\");
                changedFiles.add(s);
            }
            return changedFiles;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
