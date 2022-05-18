package org.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import static org.example.Main.halfRelease;
import static org.example.Main.rootLen;


public class MetricsCalculator {

    public static void setMetrics(Commit commit){
        int releaseIndex = commit.releaseIndex;
        for(MyFile file : commit.changedFiles){
            Release rel = halfRelease.get(releaseIndex);
            for(MyFile file2: rel.files){
                String name = file2.pathname.substring(rootLen);
                if(file.pathname.equalsIgnoreCase(name)){
                    file2.nRevisions =  file2.nRevisions + 1;
                    file2.authors.add(commit.author);
                    file2.locTouched = file2.locTouched + file.locTouched;
                    file2.locAddedList.add(file.locAdded);
                    file2.locAdded = file2.locAdded + file.locAdded;
                    file2.chgSetSize = file2.chgSetSize + commit.changedFiles.size() - 1;
                    file2.chgSetSizeList.add(commit.changedFiles.size() - 1);
                    break;
                }
            }
        }




    }
    public static void setLoc(Release release){
        BufferedReader reader;
        try {

            for(MyFile f: release.files) {

                reader = new BufferedReader(new FileReader(f.pathname));
                int lines = 0;
                boolean noExit = true;
                while (noExit) {
                    String s = reader.readLine();
                    if(s == null) noExit = false;
                    else lines++;
                }
                reader.close();
                f.loc = lines;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
