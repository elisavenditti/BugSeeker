package org.example;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
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


}
