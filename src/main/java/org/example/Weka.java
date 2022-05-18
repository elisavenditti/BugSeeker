package org.example;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;
import weka.core.Instances;
import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SpreadSubsample;


import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

import static org.example.Main.halfRelease;

public class Weka {
    /*
     *  How to use WEKA API in Java
     *  Copyright (C) 2014
     *  @author Dr Noureddin M. Sadawi (noureddin.sadawi@gmail.com)
     *
     *  This program is free software: you can redistribute it and/or modify
     *  it as you wish ...
     *  I ask you only, as a professional courtesy, to cite my name, web page
     *  and my YouTube Channel!
     *
     */
    private ArrayList<String> arffTraining;
    private ArrayList<String> arffTesting;
    private ArrayList<ArrayList<Object>> row;
    private String projName;

    public void createArff(int nRelease, Boolean syncope) throws IOException {
        String trainingName;
        String format = ".xlsx";
        String testNameAdd = "-testing";
        String rootPath = "C:\\Users\\Elisa Venditti\\Desktop\\ISW2\\BugSeeker\\";
        ArrayList<String> csvNames = new ArrayList<>();
        arffTraining = new ArrayList<>();
        arffTesting = new ArrayList<>();
        String header = "% 1. Title: Iris Plants Database\n% \n" +
                "% 2. Sources:\n%      (a) Creator: Elisa Venditti\n" +
                "%      (b) Donor: Elisa Venditti\n" +
                "%      (c) Date: May, 2022\n% \n@RELATION buggy\n\n" +
                "@ATTRIBUTE size  NUMERIC\n" +
                "@ATTRIBUTE locTouched   NUMERIC\n" +
                "@ATTRIBUTE revisionNumber  NUMERIC\n" +
                "@ATTRIBUTE authorNumber   NUMERIC\n" +
                "@ATTRIBUTE locAdded   NUMERIC\n" +
                "@ATTRIBUTE maxLocAdded   NUMERIC\n" +
                "@ATTRIBUTE avgLocAdded   NUMERIC\n" +
                "@ATTRIBUTE chgSetSize   NUMERIC\n" +
                "@ATTRIBUTE maxChgSetSize   NUMERIC\n" +
                "@ATTRIBUTE avgChgSetSize   NUMERIC\n" +
                "@ATTRIBUTE class        {Yes, No}\n";

        if(syncope) trainingName = "syncope";
        else trainingName= "bookkeeper";

        // # walkForward step = # releases - 1
        for(int walkStep=0; walkStep < nRelease-1; walkStep++){
            String train = rootPath+trainingName+walkStep+format;
            csvNames.add(train);
            arffTraining.add(train.substring(0, train.length()-4)+"arff");
            String test = rootPath+trainingName+testNameAdd+walkStep+format;
            csvNames.add(test);
            arffTesting.add(test.substring(0, test.length()-4)+"arff");
        }

        for(String path: csvNames){

            FileInputStream inputStream = new FileInputStream(path);


            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet firstSheet = workbook.getSheetAt(0);
            Iterator<Row> iterator = firstSheet.iterator();
            String content = header+"@DATA\n";
            boolean first = true;
            while (iterator.hasNext()) {
                Row nextRow = iterator.next();
                if (first){
                    first=false;
                    continue;
                }

                double sizeCell = nextRow.getCell(2).getNumericCellValue();
                double locTouchedCell = nextRow.getCell(3).getNumericCellValue();
                double nRevCell = nextRow.getCell(4).getNumericCellValue();
                double nAuthorsCell = nextRow.getCell(5).getNumericCellValue();
                double locAddedCell = nextRow.getCell(6).getNumericCellValue();
                double maxLocAddedCell = nextRow.getCell(7).getNumericCellValue();
                double avgLocAddedCell = nextRow.getCell(8).getNumericCellValue();
                double chgSetSizeCell = nextRow.getCell(9).getNumericCellValue();
                double maxChgSetSizeCell = nextRow.getCell(10).getNumericCellValue();
                double avgChgSetSizeCell = nextRow.getCell(11).getNumericCellValue();
                String bugCell = nextRow.getCell(12).getStringCellValue();

                content = content + sizeCell + ","+ locTouchedCell + ","+ nRevCell + ","+ nAuthorsCell +
                        ","+ locAddedCell + ","+ maxLocAddedCell + ","+ avgLocAddedCell + ","
                        + chgSetSizeCell + ","+ maxChgSetSizeCell + ","+ avgChgSetSizeCell + ","+ bugCell + "\n";
            }

            String filePath = path.substring(0, path.length()-4)+"arff";
            File file = new File(filePath);
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            bw.write(content);
            bw.close();



            workbook.close();
            inputStream.close();

        }
    }

    public void walkforwardStep(String trainArffPathname, String testArffPathname, int trainingIndex) throws Exception{


        DataSource source1 = new DataSource(trainArffPathname);
        Instances training = source1.getDataSet();
        DataSource source2 = new DataSource(testArffPathname);
        Instances testing = source2.getDataSet();

        int numAttr = training.numAttributes();
        training.setClassIndex(numAttr - 1);
        testing.setClassIndex(numAttr - 1);

        NaiveBayes classifierNB = new NaiveBayes();
        RandomForest classifierRF = new RandomForest();
        IBk classifierIBK = new IBk();


        classifierNB.buildClassifier(training);
        classifierRF.buildClassifier(training);
        classifierIBK.buildClassifier(training);

        Evaluation evalNB = new Evaluation(testing);
        evalNB.evaluateModel(classifierNB, testing);
        addRow(trainingIndex, evalNB, "Naive Bayes");

        Evaluation evalRF = new Evaluation(testing);
        evalRF.evaluateModel(classifierRF, testing);
        addRow(trainingIndex, evalRF, "Random Forest");

        Evaluation evalIBK = new Evaluation(testing);
        evalIBK.evaluateModel(classifierIBK, testing);
        addRow(trainingIndex, evalIBK, "IBk");

    }

    private void addRow(int trainingIndex, Evaluation eval, String classifier){
        ArrayList<Object> performance = new ArrayList<>();
        performance.add(projName);
        String trainingReleases = "";
        for(int index=0; index<=trainingIndex; index++){
            trainingReleases = trainingIndex +", " + halfRelease.get(index).name;
        }
        performance.add(trainingReleases);
        performance.add(classifier);
        double precision = eval.precision(0);
        double recall = eval.recall(0);
        double auc = eval.areaUnderROC(0);
        double kappa = eval.kappa();
        performance.add(precision);
        performance.add(recall);
        performance.add(auc);
        performance.add(kappa);
        row.add(performance);
    }

    private void setUp(Boolean syncope){
        row = new ArrayList<>();
        ArrayList<Object> firstElement = new ArrayList<>();
        firstElement.add("Dataset");
        firstElement.add("#TrainingRelease");
        firstElement.add("Classifier");
        firstElement.add("Precision");
        firstElement.add("Recall");
        firstElement.add("AUC");
        firstElement.add("Kappa");
        row.add(firstElement);
        if(syncope) projName="Syncope";
        else projName="Bookkeeper";
    }
    private void report() throws IOException {
        String excelName = projName.toLowerCase() + "-report.xlsx";
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet(projName + " weka");
        int rowCount = 0;


        for (ArrayList<Object> r: row) {

            Row row = sheet.createRow(rowCount);
            Cell datasetCell = row.createCell(0);
            Cell trainingReleaseCell = row.createCell(1);
            Cell classifierCell = row.createCell(2);
            Cell precisionCell = row.createCell(3);
            Cell recallCell = row.createCell(4);
            Cell aucCell = row.createCell(5);
            Cell kappaCell = row.createCell(6);

            datasetCell.setCellValue((String) r.get(0));
            trainingReleaseCell.setCellValue((String) r.get(1));
            classifierCell.setCellValue((String) r.get(2));
            precisionCell.setCellValue(r.get(3).toString());
            recallCell.setCellValue(r.get(4).toString());
            aucCell.setCellValue(r.get(5).toString());
            kappaCell.setCellValue(r.get(6).toString());
            rowCount++;

        }

        FileOutputStream outputStream = new FileOutputStream(excelName);
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();


    }
    public void walkforward(int nRelease, Boolean syncope){
        int numIterations = nRelease - 1;
        setUp(syncope);

        for(int i=0; i<numIterations; i++){
            try {
                walkforwardStep(arffTraining.get(i), arffTesting.get(i), i+1);
                System.out.println("[walkforward step] "+i);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        try {
            report();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

//        double precisionMean=0,recallMean=0, aucMean=0, kappaMean=0;
//        for(ArrayList<Double> singlePerformance: performances){
//            precisionMean = precisionMean+singlePerformance.get(0);
//            recallMean = recallMean+singlePerformance.get(1);
//            aucMean = aucMean+singlePerformance.get(2);
//            kappaMean = kappaMean+singlePerformance.get(3);
//        }
//        precisionMean = precisionMean/4;
//        recallMean = recallMean/4;
//        aucMean = aucMean/4;
//        kappaMean = kappaMean/4;
//        System.out.println("PRECISIONE: " + precisionMean + "\nRECALL: " + recallMean +
//                "\nACCURATEZZA: " + aucMean + "\nKAPPA: " + kappaMean);


    }






    public void filter(String trainingArff, String testingArff) throws Exception{
        DataSource source2 = new DataSource(testingArff);
        Instances testingNoFilter = source2.getDataSet();


        DataSource source = new DataSource(trainingArff);
        Instances noFilterTraining = source.getDataSet();

        // create Filter which use an EVALUATOR and a SEARCH ALGORITHM
        AttributeSelection filter = new AttributeSelection();
        CfsSubsetEval eval = new CfsSubsetEval();
        GreedyStepwise search = new GreedyStepwise();
        search.setSearchBackwards(true);            // backward
        filter.setEvaluator(eval);
        filter.setSearch(search);
        filter.setInputFormat(noFilterTraining);

        Instances filteredTraining = Filter.useFilter(noFilterTraining, filter);

        int numAttrNoFilter = noFilterTraining.numAttributes();
        noFilterTraining.setClassIndex(numAttrNoFilter - 1);
        testingNoFilter.setClassIndex(numAttrNoFilter - 1);

        int numAttrFiltered = filteredTraining.numAttributes();


        System.out.println("No filter attr: " + numAttrNoFilter);
        System.out.println("Filtered attr: " + numAttrFiltered);

        RandomForest classifier = new RandomForest();


        //evaluation with no filtered
        Evaluation evalClass = new Evaluation(testingNoFilter);
        classifier.buildClassifier(noFilterTraining);
        evalClass.evaluateModel(classifier, testingNoFilter);

        System.out.println("Precision no filter = "+evalClass.precision(0));
        System.out.println("Recall no filter = "+evalClass.recall(0));
        System.out.println("AUC no filter = "+evalClass.areaUnderROC(0));
        System.out.println("Kappa no filter = "+evalClass.kappa());

        //evaluation with filtered
        filteredTraining.setClassIndex(numAttrFiltered - 1);
        Instances testingFiltered = Filter.useFilter(testingNoFilter, filter);
        testingFiltered.setClassIndex(numAttrFiltered - 1);
        classifier.buildClassifier(filteredTraining);
        evalClass.evaluateModel(classifier, testingFiltered);

        System.out.println("Precision filtered = "+evalClass.precision(0));
        System.out.println("Recall filtered = "+evalClass.recall(0));
        System.out.println("AUC filtered = "+evalClass.areaUnderROC(1));
        System.out.println("Kappa filtered = "+evalClass.kappa());


    }

    public void sampling(String trainingArff, String testingArff) throws Exception {
        //load datasets
        DataSource source1 = new DataSource(trainingArff);
        Instances training = source1.getDataSet();
        DataSource source2 = new DataSource(testingArff);
        Instances testing = source2.getDataSet();



        int numAttr = training.numAttributes();
        training.setClassIndex(numAttr - 1);
        testing.setClassIndex(numAttr - 1);



        RandomForest RandomForest = new RandomForest();

        RandomForest.buildClassifier(training);


        Evaluation eval = new Evaluation(testing);
        eval.evaluateModel(RandomForest, testing); //not sampled





        Resample resample = new Resample();
        resample.setInputFormat(training);
        FilteredClassifier fc = new FilteredClassifier();


        RandomForest RandomForest2 = new RandomForest();
        fc.setClassifier(RandomForest2);

				/*fc.setFilter(resample);
				eventual parameters setting omitted
				*/

			   /* SMOTE smote = new SMOTE();
				smote.setInputFormat(training);
				fc.setFilter(smote);
				*/

        SpreadSubsample spreadSubsample = new SpreadSubsample();
        String[] opts = new String[]{ "-M", "1.0"};
        spreadSubsample.setOptions(opts);
        fc.setFilter(spreadSubsample);

        fc.buildClassifier(training);
        Evaluation eval2 = new Evaluation(testing);
        eval2.evaluateModel(fc, testing); //sampled

        System.out.println("Correct% nonsampled = "+eval.pctCorrect());
        System.out.println("Correct% sampled= "+eval2.pctCorrect()+ "\n");

        System.out.println("Precision nonsampled= "+eval.precision(1));
        System.out.println("Precision sampled= "+eval2.precision(1)+ "\n");

        System.out.println("Recall nonsampled= "+eval.recall(1));
        System.out.println("Recall sampled= "+eval2.recall(1)+ "\n");


    }

}
