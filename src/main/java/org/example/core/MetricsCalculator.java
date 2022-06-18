package org.example.core;

import org.example.Main;
import org.example.entity.Commit;
import org.example.entity.MyFile;
import org.example.entity.Release;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MetricsCalculator {
    private MetricsCalculator(){}

    public static void setMetrics(Commit commit){
        int releaseIndex = commit.getReleaseIndex();
        for(MyFile file : commit.getChangedFiles()){
            Release rel = Main.getHalfRelease().get(releaseIndex);

            for(MyFile file2: rel.getFiles()){

                String name = file2.getPathname().substring(Main.getRootLen());
                if(file.getPathname().equalsIgnoreCase(name)){
                    file2.setnRevisions(file2.getnRevisions() + 1);
                    file2.addItemInAuthors(commit.getAuthor());
                    file2.setLocTouched(file2.getLocTouched() + file.getLocTouched());

                    file2.addItemInLocAddedList(file.getLocAdded());
                    file2.setLocAdded(file2.getLocAdded() + file.getLocAdded());
                    file2.setChgSetSize(file2.getChgSetSize() + commit.getChangedFiles().size() - 1);
                    file2.addItemInChgSetSizeList(commit.getChangedFiles().size() - 1);
                    break;
                }
            }
        }




    }
    public static void setLoc(Release release){
        BufferedReader reader;
        try {

            for(MyFile f: release.getFiles()) {

                reader = new BufferedReader(new FileReader(f.getPathname()));
                int lines = 0;
                boolean noExit = true;
                while (noExit) {
                    String s = reader.readLine();
                    if(s == null) noExit = false;
                    else lines++;
                }
                reader.close();
                f.setLoc(lines);
            }
        } catch (IOException e) {
            Logger logger = Logger.getLogger(MetricsCalculator.class.getName());
            logger.log(Level.INFO, e.getMessage());
        }
    }
}
