package org.example;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import static org.example.Main.halfRelease;
import static org.example.Main.rootLen;


public class MetricsCalculator {

    public static void setMetrics(Commit commit){
        int releaseIndex = commit.releaseIndex;
        for(MyFile file : commit.changedFiles){
            Release rel = halfRelease.get(releaseIndex);
            if(releaseIndex==1)
                System.out.println(file.pathname);
            for(MyFile file2: rel.files){
                String name = file2.pathname.substring(rootLen);//, file2.pathname.length());
                if(file.pathname.equalsIgnoreCase(name)){
                    if(releaseIndex == 1 && file.pathname.equalsIgnoreCase("syncope\\build-tools\\src\\main\\java\\org\\apache\\syncope\\buildtools\\H2StartStopListener.java"))
                        System.out.println("ciao");
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
        BufferedReader reader = null;
        try {

            for(MyFile f: release.files) {

                reader = new BufferedReader(new FileReader(f.pathname));
                int lines = 0;
                while (true) {
                    if (!(reader.readLine() != null)) break;
                    lines++;
                }
                reader.close();
                f.loc = lines;
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
