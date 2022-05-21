package org.example;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class Excel {
    private String rootDirectory;
    private int rootLen;

    public Excel(String rootDirectory){
        this.rootDirectory = rootDirectory+"\\";
        this.rootLen = this.rootDirectory.length();
    }


    public List<MyFile> listOfJavaFile(String rootDirName){
        File rootDir = new File(rootDirName);
        ArrayList<MyFile> resultList = new ArrayList<>();

        File[] fList = rootDir.listFiles();
        if(fList == null) new ArrayList<>();
        for (File file : fList) {
            if (file.isFile() && file.getName().contains(".java")) {
                MyFile myFile = new MyFile(file.getAbsolutePath());
                resultList.add(myFile);
            } else if (file.isDirectory() && !(file.getName().contains("test")) && !(file.getName().equalsIgnoreCase("target"))) {
                resultList.addAll(listOfJavaFile(file.getAbsolutePath()));
            }
        }
        return resultList;

    }

    private int insertCells(XSSFSheet sheet, int rowCount, Release release){
        String name;
        for (MyFile myFile : release.getFiles()) {
            name = myFile.getPathname();
            Row row = sheet.createRow(rowCount);

            int j = name.length();
            String shortName;
            String buggy;
            Cell releaseCell = row.createCell(0);
            Cell nameCell = row.createCell(1);
            Cell sizeCell = row.createCell(2);
            Cell locTouchedCell = row.createCell(3);
            Cell nRevCell = row.createCell(4);
            Cell nAuthorsCell = row.createCell(5);
            Cell locAddedCell = row.createCell(6);
            Cell maxLocAddedCell = row.createCell(7);
            Cell avgLocAddedCell = row.createCell(8);
            Cell chgSetSizeCell = row.createCell(9);
            Cell maxChgSetSizeCell = row.createCell(10);
            Cell avgChgSetSizeCell = row.createCell(11);
            Cell bugCell = row.createCell(12);

            if(rowCount==0){
                releaseCell.setCellValue("Version");
                nameCell.setCellValue("Filename");
                sizeCell.setCellValue("LOC");
                locTouchedCell.setCellValue("LOC Touched");
                nRevCell.setCellValue("nRevisions");
                nAuthorsCell.setCellValue("nAuthors");
                locAddedCell.setCellValue("LOC Added");
                maxLocAddedCell.setCellValue("max LocAdded");
                avgLocAddedCell.setCellValue("avgLocAdded");
                chgSetSizeCell.setCellValue("chgSetSize");
                maxChgSetSizeCell.setCellValue("maxChgSetSize");
                avgChgSetSizeCell.setCellValue("avgChgSetSize");
                bugCell.setCellValue("Buggy");
            }else {

                releaseCell.setCellValue(release.getName());
                shortName = name.substring(rootLen, j);
                nameCell.setCellValue(shortName);

                boolean bug = false;
                for (MyFile buggyFile : release.getBuggyFiles()) {
                    if (buggyFile.getPathname().equalsIgnoreCase(shortName)) {
                        bug = true;
                        break;
                    }
                }
                if (bug)
                    buggy = "Yes";
                else
                    buggy = "No";

                int avgLocAdded = 0;
                int avgChgSetSize = 0;
                int maxLocAdded = 0;
                int maxChgSetSize = 0;
                if (!myFile.getLocAddedList().isEmpty()) {
                    for (Integer add : myFile.getLocAddedList()) {
                        if (add > maxLocAdded) maxLocAdded = add;
                        avgLocAdded = avgLocAdded + add;
                    }
                    avgLocAdded = avgLocAdded / (myFile.getLocAddedList().size());
                }
                if (!myFile.getChgSetSizeList().isEmpty()) {
                    for (Integer add : myFile.getChgSetSizeList()) {
                        if (add > maxChgSetSize) maxChgSetSize = add;
                        avgChgSetSize = avgChgSetSize + add;
                    }
                    avgChgSetSize = avgChgSetSize / (myFile.getChgSetSizeList().size());
                }


                sizeCell.setCellValue(myFile.getLoc());
                locTouchedCell.setCellValue(myFile.getLocTouched());
                nRevCell.setCellValue(myFile.getnRevisions());
                nAuthorsCell.setCellValue(myFile.getnAuthors());
                locAddedCell.setCellValue(myFile.getLocAdded());
                maxLocAddedCell.setCellValue(maxLocAdded);
                avgLocAddedCell.setCellValue(avgLocAdded);
                chgSetSizeCell.setCellValue(myFile.getChgSetSize());
                maxChgSetSizeCell.setCellValue(maxChgSetSize);
                avgChgSetSizeCell.setCellValue(avgChgSetSize);
                bugCell.setCellValue(buggy);
            }
            rowCount++;

        }
        return rowCount;
    }


    public void populate(String projectName, int walkforwardStep)  {

        String excelName = projectName.toLowerCase() + walkforwardStep + ".xlsx";
        XSSFWorkbook workbook;
        XSSFSheet sheet;
        int rowCount = 0;

        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet(projectName + "buggy classes");
        int c=0;
        for(Release r: Main.getHalfRelease()){
            if(c>walkforwardStep) break;
            rowCount = insertCells(sheet, rowCount, r);
            c++;
        }

        Logger logger = Logger.getLogger(Excel.class.getName());
        String out = "[training "+walkforwardStep+" excel completato";
        logger.log(Level.INFO, out);


        try(FileOutputStream outputStream = new FileOutputStream(excelName)) {
            workbook.write(outputStream);
            workbook.close();
        } catch (IOException e) {
            logger.log(Level.INFO, e.getMessage());
        }

    }

    public void populateTesting(String projectName, int testReleaseIndex) throws IOException {

        int actualStep = testReleaseIndex-1;
        String excelName = projectName.toLowerCase() + "-testing" + actualStep + ".xlsx";
        XSSFWorkbook workbook;
        XSSFSheet sheet;
        int rowCount = 0;

        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet(projectName + "buggy classes");

        insertCells(sheet, rowCount, Main.getHalfRelease().get(testReleaseIndex));

        Logger logger = Logger.getLogger(Excel.class.getName());
        String out = "[training "+testReleaseIndex+"] excel completato";
        logger.log(Level.INFO, out);


        try(FileOutputStream outputStream = new FileOutputStream(excelName)) {
            workbook.write(outputStream);
            workbook.close();
        } catch (IOException e) {
            logger.log(Level.INFO, e.getMessage());
        }
    }


}
