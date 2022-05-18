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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class Prova {
    private static Repository repo;
    private static String gitPath = "C:\\Users\\Elisa Venditti\\Desktop\\provaJira\\.git";

    private static List<MyFile> compareCommitWithPrevious(String commitSha) throws MyException{

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

                if(entry.getChangeType().equals(DiffEntry.ChangeType.DELETE)){
                    String ss = entry.getOldPath().replaceAll("/","\\\\");
                    System.out.println(ss);
                }

                linesAdded = 0;
                linesDeleted = 0;
                for (Edit edit : diffFormatter.toFileHeader(entry).toEditList()) {

                    linesDeleted += edit.getEndA() - edit.getBeginA();
                    linesAdded += edit.getEndB() - edit.getBeginB();
                }
                locTouched = linesAdded + linesDeleted;
                String s = entry.getNewPath().replaceAll("/","\\\\");
                System.out.println(s);
                System.out.println("[FILE "+s+"] deleted: "+linesDeleted+" added: " + linesAdded+" total: "+locTouched);

                changedFiles.add(new MyFile(s));
            }
            return changedFiles;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] argv) {
        int index = 0;
        FileRepositoryBuilder b = new FileRepositoryBuilder();
        try {
            repo = b.setGitDir(new File(gitPath)).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        org.eclipse.jgit.api.Git git = new org.eclipse.jgit.api.Git(repo);
        try {

            Iterable<RevCommit> log = git.log().all().call();
            for(RevCommit i: log){
                PersonIdent authorIdent = i.getAuthorIdent();
                String author = authorIdent.getName();
                //System.out.println(author);
                Date authorDate = authorIdent.getWhen();
                Commit c = new Commit(i.getId().toString());

//                if(c.releaseIndex==-1 || c.releaseIndex > (halfRelease.size()-1))
//                    continue;

                String csha = c.getCommitSha();

                try {
                    System.out.println(index+"...");
                    c.changedFiles.addAll(compareCommitWithPrevious(csha));
                } catch (MyException e){
                    System.out.println(e.getMessage());
                }
                index++;

                //setRevisions(c);

            }
//            for (Release r: allRelease) {
//                if(r.index > (halfRelease.size()-1)) continue;
//                System.out.println("Release: "+r.name);
//                int k = 0;
//                for (MyFile f : r.files){
//                    System.out.println(" *file (" +k + ") - nRev: " + f.nRevisions+" nAuthors"+f.getNumberOfAuthors());
//                    k++;
//                }
//            }
            git.close();
        } catch (NoHeadException e) {
            throw new RuntimeException(e);
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }




    }
//        ArrayList<String> authors = new ArrayList<>();
//        authors.add("Elisa");
//        authors.add("Taehyung");
//        authors.add("Jungkook");
//        authors.add("Elisa");
//        authors.add("Jungkook");
//
//        long unique = authors.stream().distinct().count();
//        for(String a: authors) System.out.println(a);
//        System.out.println(unique);




}
