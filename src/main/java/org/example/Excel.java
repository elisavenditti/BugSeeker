package org.example;
import java.io.*;
import java.util.ArrayList;
import java.util.List;


import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookFactory;

import static org.example.Main.halfRelease;

public class Excel {
    private String rootDirectory;
    private int rootLen;

    public Excel(String rootDirectory){
        this.rootDirectory = rootDirectory+"\\";
        this.rootLen = this.rootDirectory.length();
    }

//    public void modifyAllVerticalCell(int x, String excelFilePath, List<String> buggyClasses){
//        String currClass, buggy;
//
//        try {
//            FileInputStream inputStream = new FileInputStream(new File(excelFilePath));
//            XSSFWorkbook workbook = (XSSFWorkbook) XSSFWorkbookFactory.create(inputStream);
//
//            XSSFSheet sheet = workbook.getSheetAt(0);
//
//
//            int rowCount = 0;
//            int lastRow = sheet.getLastRowNum()+1;
//
//            while(rowCount!=lastRow){
//                Row row = sheet.getRow(rowCount++);
//                Cell cellOfName = row.getCell(1);
//
//                Cell cell = row.createCell(x);
//                currClass = cellOfName.getStringCellValue();
//                if(buggyClasses.contains(currClass))
//                    buggy = "Yes";
//                else
//                    buggy = "No";
//                cell.setCellValue(buggy);
//
//            }
//
//            inputStream.close();
//
//            FileOutputStream outputStream = new FileOutputStream(excelFilePath);
//            workbook.write(outputStream);
//            workbook.close();
//            outputStream.close();
//
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//    }

//    public void modifyBuggyCellOfRelease(String release, int x, String excelFilePath, List<String> buggyClasses){
//        String currClass, buggy;
//
//        try {
//            FileInputStream inputStream = new FileInputStream(excelFilePath);
//            XSSFWorkbook workbook = (XSSFWorkbook) XSSFWorkbookFactory.create(inputStream);
//            XSSFSheet sheet = workbook.getSheetAt(0);
//
//            int rowCount = 0;
//            int lastRow = sheet.getLastRowNum()+1;
//
//            while(rowCount!=lastRow){
//                Row currRow = sheet.getRow(rowCount++);
//                Cell releaseCell = currRow.getCell(0);
//                String currRelease = releaseCell.getStringCellValue();
//
//                if(currRelease.equalsIgnoreCase(release)){
//
//                    Cell nameCell, bugCell;
//                    nameCell = currRow.getCell(1);
//                    bugCell = currRow.createCell(x);
//                    currClass = nameCell.getStringCellValue();
//
//                    if (buggyClasses.contains(currClass))
//                        buggy = "Yes";
//                    else
//                        buggy = "No";
//                    bugCell.setCellValue(buggy);
//                }
//
//            }
//
//            inputStream.close();
//
//            FileOutputStream outputStream = new FileOutputStream(excelFilePath);
//            workbook.write(outputStream);
//            workbook.close();
//            outputStream.close();
//
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//    }
    public List<String> listOfJavaFile(String rootDirName){
        File rootDir = new File(rootDirName);
        List<String> resultList = new ArrayList<>();

        File[] fList = rootDir.listFiles();
        for (File file : fList) {
            if (file.isFile() && file.getName().contains(".java")) {
                resultList.add(file.getAbsolutePath());
            } else if (file.isDirectory() && !(file.getName().equalsIgnoreCase("target"))) {
                resultList.addAll(listOfJavaFile(file.getAbsolutePath()));
            }
        }
        return resultList;

    }

    private int insertCells(XSSFSheet sheet, int rowCount, Release release){
        for (String name : release.files) {
            Row row = sheet.createRow(rowCount);

            int j = name.length();
            String shortName;
            String buggy;
            Cell releaseCell = row.createCell(0);
            Cell nameCell = row.createCell(1);
            Cell bugCell = row.createCell(2);
            releaseCell.setCellValue(release.name);
            shortName = name.substring(rootLen,j);
            nameCell.setCellValue(shortName);

            String currRelease = releaseCell.getStringCellValue();

            if(release.buggyFiles.contains(shortName))
                buggy = "Yes";
            else
                buggy = "No";

            bugCell.setCellValue(buggy);

            rowCount++;

        }
        return rowCount;
    }

//    public String addClasses(String projectName, List<String> classNames, String release) throws IOException {
//
//        String excelName = projectName.toLowerCase() + ".xlsx";
//        File f = new File("./" + excelName);
//        XSSFWorkbook workbook;
//        XSSFSheet sheet;
//        int rowCount = 0;
//
//        if(!f.exists()) {
//            workbook = new XSSFWorkbook();
//            sheet = workbook.createSheet(projectName + "buggy classes");
//            insertCells(sheet, classNames, rowCount, release);
//        }else{
//
//            FileInputStream inputStream = new FileInputStream(excelName);
//            workbook = (XSSFWorkbook) XSSFWorkbookFactory.create(inputStream);
//            sheet = workbook.getSheetAt(0);
//            rowCount = sheet.getLastRowNum()+1;
//            insertCells(sheet, classNames, rowCount, release);
//            inputStream.close();
//        }
//
//        FileOutputStream outputStream = new FileOutputStream(excelName);
//        workbook.write(outputStream);
//        workbook.close();
//        outputStream.close();
//        return excelName;
//    }

    public String populate(String projectName) throws IOException {

        String excelName = projectName.toLowerCase() + ".xlsx";
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
    }






    public String createExcelFile(String projectName, List<String> classNames){
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet(projectName + "buggy classes");

        int rowCount = 0;

        if (rootDirectory==null){
            System.out.println("ERRORE INIZIALIZZAZIONE ROOT DIRECTORY");
            return null;
        }

        for (String name : classNames) {
            Row row = sheet.createRow(rowCount++);

            int j = name.length();

            Cell cell = row.createCell(0);
            cell.setCellValue((Integer) 1);
            Cell cell2 = row.createCell(1);
            cell2.setCellValue(name.substring(rootLen,j));

        }


        try (FileOutputStream outputStream = new FileOutputStream(projectName + ".xlsx")) {
            workbook.write(outputStream);
            workbook.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return projectName + ".xlsx";
    }
    public static void main(String[] Argv){

    }
}
