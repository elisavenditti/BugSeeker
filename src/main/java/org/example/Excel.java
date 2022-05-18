package org.example;
import java.io.*;
import java.util.ArrayList;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import static org.example.Main.halfRelease;

public class Excel {
    private String rootDirectory;
    private int rootLen;

    public Excel(String rootDirectory){
        this.rootDirectory = rootDirectory+"\\";
        this.rootLen = this.rootDirectory.length();
    }


    public ArrayList<MyFile> listOfJavaFile(String rootDirName){
        File rootDir = new File(rootDirName);
        ArrayList<MyFile> resultList = new ArrayList<>();

        File[] fList = rootDir.listFiles();
        if(fList == null) return null;
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
        for (MyFile myFile : release.files) {
            name = myFile.pathname;
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

                releaseCell.setCellValue(release.name);
                shortName = name.substring(rootLen, j);
                nameCell.setCellValue(shortName);

                boolean bug = false;
                for (MyFile buggyFile : release.buggyFiles) {
                    if (buggyFile.pathname.equalsIgnoreCase(shortName)) {
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
                if (myFile.locAddedList.size() != 0) {
                    for (Integer add : myFile.locAddedList) {
                        if (add > maxLocAdded) maxLocAdded = add;
                        avgLocAdded = avgLocAdded + add;
                    }
                    avgLocAdded = avgLocAdded / (myFile.locAddedList.size());
                }
                if (myFile.chgSetSizeList.size() != 0) {
                    for (Integer add : myFile.chgSetSizeList) {
                        if (add > maxChgSetSize) maxChgSetSize = add;
                        avgChgSetSize = avgChgSetSize + add;
                    }
                    avgChgSetSize = avgChgSetSize / (myFile.chgSetSizeList.size());
                }


                sizeCell.setCellValue(myFile.loc);
                locTouchedCell.setCellValue(myFile.locTouched);
                nRevCell.setCellValue(myFile.nRevisions);
                nAuthorsCell.setCellValue(myFile.nAuthors);
                locAddedCell.setCellValue(myFile.locAdded);
                maxLocAddedCell.setCellValue(maxLocAdded);
                avgLocAddedCell.setCellValue(avgLocAdded);
                chgSetSizeCell.setCellValue(myFile.chgSetSize);
                maxChgSetSizeCell.setCellValue(maxChgSetSize);
                avgChgSetSizeCell.setCellValue(avgChgSetSize);
                bugCell.setCellValue(buggy);
            }
            rowCount++;

        }
        return rowCount;
    }


    public void populate(String projectName, int walkforwardStep) throws IOException {

        String excelName = projectName.toLowerCase() + walkforwardStep + ".xlsx";
        XSSFWorkbook workbook;
        XSSFSheet sheet;
        int rowCount = 0;

        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet(projectName + "buggy classes");
        int c=0;
        for(Release r: halfRelease){
            if(c>walkforwardStep) break;
            rowCount = insertCells(sheet, rowCount, r);
            c++;
        }

        System.out.println("[training "+walkforwardStep+"] excel completato");


        FileOutputStream outputStream = new FileOutputStream(excelName);
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();
    }

    public void populateTesting(String projectName, int testReleaseIndex) throws IOException {

        int actualStep = testReleaseIndex-1;
        String excelName = projectName.toLowerCase() + "-testing" + actualStep + ".xlsx";
        XSSFWorkbook workbook;
        XSSFSheet sheet;
        int rowCount = 0;

        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet(projectName + "buggy classes");

        insertCells(sheet, rowCount, halfRelease.get(testReleaseIndex));


        System.out.println("[training "+testReleaseIndex+"] excel completato");


        FileOutputStream outputStream = new FileOutputStream(excelName);
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();
    }

    /*public String populate(String projectName) throws IOException {

        String excelName = projectName.toLowerCase() + walkforwardStep + ".xlsx";
        XSSFWorkbook workbook;
        XSSFSheet sheet;
        int rowCount = 0;

        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet(projectName + "buggy classes");
        int c=0;
        for(Release r: halfRelease){
            System.out.println(c+"* Popolo celle excel per la release");
            rowCount = insertCells(sheet, rowCount, r);
            c++;
        }


        FileOutputStream outputStream = new FileOutputStream(excelName);
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();
        return excelName;
    }*/


}
