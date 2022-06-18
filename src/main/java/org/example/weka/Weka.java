package org.example.weka;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.Main;
import weka.attributeSelection.BestFirst;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.AttributeStats;
import weka.core.Instances;
import weka.attributeSelection.CfsSubsetEval;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SpreadSubsample;


import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


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
    private String rootPath;

    public Weka(String rootpath){
        this.rootPath=rootpath;
    }
    
    private String getSeparatorToAppend(int i, int size){
        String separator = "";
        if(i==size-1) separator = "\n";
        else if(i!=0) separator = ",";
        return separator;
    }
    public void createArff(int nRelease, boolean syncope) {
        String trainingName;
        String format = ".xlsx";
        String testNameAdd = "-testing";
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
            String train = this.rootPath+trainingName+walkStep+format;
            csvNames.add(train);
            arffTraining.add(train.substring(0, train.length()-4)+"arff");
            String test = this.rootPath+trainingName+testNameAdd+walkStep+format;
            csvNames.add(test);
            arffTesting.add(test.substring(0, test.length()-4)+"arff");
        }

        for(String path: csvNames){

            try(FileInputStream inputStream = new FileInputStream(path)) {
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

                    ArrayList<String> arrayOfStrings = new ArrayList<>();
                    arrayOfStrings.add(content);
                    arrayOfStrings.add(String.valueOf(sizeCell));
                    arrayOfStrings.add(String.valueOf(locTouchedCell));
                    arrayOfStrings.add(String.valueOf(nRevCell));
                    arrayOfStrings.add(String.valueOf(nAuthorsCell));
                    arrayOfStrings.add(String.valueOf(locAddedCell));
                    arrayOfStrings.add(String.valueOf(maxLocAddedCell));
                    arrayOfStrings.add(String.valueOf(avgLocAddedCell));
                    arrayOfStrings.add(String.valueOf(chgSetSizeCell));
                    arrayOfStrings.add(String.valueOf(maxChgSetSizeCell));
                    arrayOfStrings.add(String.valueOf(avgChgSetSizeCell));
                    arrayOfStrings.add(bugCell);
                    StringBuilder bld = new StringBuilder();
                    for (int i = 0; i < arrayOfStrings.size(); i++) {
                        bld.append(arrayOfStrings.get(i));
                        bld.append(getSeparatorToAppend(i,arrayOfStrings.size()));
                    }
                    String str = bld.toString();

                    content = str;
                }

                String filePath = path.substring(0, path.length()-4)+"arff";
                File file = new File(filePath);
                BufferedWriter bw = new BufferedWriter(new FileWriter(file));
                bw.write(content);
                bw.close();



                workbook.close();

            } catch (IOException e) {
                Logger logger = Logger.getLogger(Weka.class.getName());
                logger.log(Level.INFO, e.getMessage());
            }

        }
    }

    private List<String> labels(List<Technique> technique){
        List<String> ret = new ArrayList<>();
        String filter = "No";
        String balancing = "No";
        String sensitivity = "No";
        if(techniqueContains(Technique.FEATURESELECTION, technique)) filter = "Filtered";
        if(techniqueContains(Technique.UNDERSAMPLING, technique)) balancing = "Undersampling";
        if(techniqueContains(Technique.OVERSAMPLING, technique)) balancing = "Oversampling";
        if(techniqueContains(Technique.SENSITIVELEARNING, technique)) sensitivity = "Sensitive Learning";
        if(techniqueContains(Technique.SENSITIVETHRESHOLD, technique)) sensitivity = "Sensitive Threshold";
        ret.add(filter);
        ret.add(balancing);
        ret.add(sensitivity);
        return ret;
    }

    private void addRow(int trainingIndex, Evaluation eval, String classifier, int[] bugginess, List<Technique> technique){
        ArrayList<Object> performance = new ArrayList<>();
        performance.add(projName);
        List<String> labels = labels(technique);
        String filter = labels.get(0);
        String balancing = labels.get(1);
        String sensitivity = labels.get(2);


        // Raggruppare tutte le release che fanno parte del training
        String trainingReleases = "";
        for(int index=0; index<=trainingIndex; index++){

            ArrayList<String> arrayOfStrings = new ArrayList<>();
            arrayOfStrings.add(trainingReleases);
            arrayOfStrings.add(", ");
            arrayOfStrings.add(Main.getHalfRelease().get(index).getName());
            StringBuilder bld = new StringBuilder();
            for (int i = 0; i < arrayOfStrings.size(); i++) {
                bld.append(arrayOfStrings.get(i));
            }
            String str = bld.toString();
            trainingReleases = str;
        }
        performance.add(trainingReleases);

        List<Double> metrics = getMetrics(trainingIndex, bugginess, eval);

        performance.add(metrics.get(0));
        performance.add(metrics.get(1));
        performance.add(metrics.get(2));
        performance.add(classifier);
        performance.add(balancing);
        performance.add(filter);
        performance.add(sensitivity);
        performance.add(metrics.get(3));
        performance.add(metrics.get(4));
        performance.add(metrics.get(5));
        performance.add(metrics.get(6));
        double precision = eval.precision(0);
        double recall = eval.recall(0);
        double auc = eval.areaUnderROC(0);
        double kappa = eval.kappa();
        double accuracy = eval.pctCorrect();
        performance.add(precision);
        performance.add(recall);
        performance.add(auc);
        performance.add(kappa);
        performance.add(accuracy);
        row.add(performance);
    }

    private void setUp(boolean syncope){
        row = new ArrayList<>();
        ArrayList<Object> firstElement = new ArrayList<>();
        firstElement.add("Dataset");
        firstElement.add("#TrainingRelease");
        firstElement.add("%training");
        firstElement.add("%defectiveInTraining");
        firstElement.add("%defectiveInTesting");
        firstElement.add("Classifier");
        firstElement.add("Balancing");
        firstElement.add("FeatureSelection");
        firstElement.add("Sensitivity");
        firstElement.add("TP");
        firstElement.add("FP");
        firstElement.add("TN");
        firstElement.add("FN");
        firstElement.add("Precision");
        firstElement.add("Recall");
        firstElement.add("AUC");
        firstElement.add("Kappa");
        firstElement.add("Accuracy");
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

            Row excelRow = sheet.createRow(rowCount);
            Cell datasetCell = excelRow.createCell(0);
            Cell trainingReleaseCell = excelRow.createCell(1);
            Cell percTrainingCell = excelRow.createCell(2);
            Cell percBuggyTrainingCell = excelRow.createCell(3);
            Cell percBuggyTestingCell = excelRow.createCell(4);
            Cell classifierCell = excelRow.createCell(5);
            Cell balancingCell = excelRow.createCell(6);
            Cell featureSelectionCell = excelRow.createCell(7);
            Cell sensitivityCell = excelRow.createCell(8);
            Cell tpCell = excelRow.createCell(9);
            Cell fpCell = excelRow.createCell(10);
            Cell tnCell = excelRow.createCell(11);
            Cell fnCell = excelRow.createCell(12);
            Cell precisionCell = excelRow.createCell(13);
            Cell recallCell = excelRow.createCell(14);
            Cell aucCell = excelRow.createCell(15);
            Cell kappaCell = excelRow.createCell(16);
            Cell accuracyCell = excelRow.createCell(17);



            datasetCell.setCellValue((String) r.get(0));
            trainingReleaseCell.setCellValue((String) r.get(1));
            percTrainingCell.setCellValue(r.get(2).toString());
            percBuggyTrainingCell.setCellValue(r.get(3).toString());
            percBuggyTestingCell.setCellValue(r.get(4).toString());
            classifierCell.setCellValue((String) r.get(5));
            balancingCell.setCellValue((String) r.get(6));
            featureSelectionCell.setCellValue((String) r.get(7));
            sensitivityCell.setCellValue((String) r.get(8));
            tpCell.setCellValue(r.get(9).toString());
            fpCell.setCellValue(r.get(10).toString());
            tnCell.setCellValue(r.get(11).toString());
            fnCell.setCellValue(r.get(12).toString());
            precisionCell.setCellValue(r.get(13).toString());
            recallCell.setCellValue(r.get(14).toString());
            aucCell.setCellValue(r.get(15).toString());
            kappaCell.setCellValue(r.get(16).toString());
            accuracyCell.setCellValue(r.get(17).toString());
            rowCount++;

        }

        try(FileOutputStream outputStream = new FileOutputStream(excelName)) {
            workbook.write(outputStream);
            workbook.close();
        } catch (IOException e) {
            Logger logger = Logger.getLogger(Weka.class.getName());
            logger.log(Level.INFO, e.getMessage());
        }


    }


    public void walkforward(int nRelease, Boolean syncope){
        int numIterations = nRelease - 1;
        setUp(syncope);

        for(int i=0; i<numIterations; i++){
            try {

                walkforwardStep(arffTraining.get(i), arffTesting.get(i), i);
                Logger logger = Logger.getLogger(Weka.class.getName());
                String out ="[walkforward step] "+i;
                logger.log(Level.INFO, out);
            } catch (Exception e) {
                Logger logger = Logger.getLogger(Weka.class.getName());
                logger.log(Level.INFO, e.getMessage());
            }
        }

        try {
            report();
        } catch (IOException e) {
            Logger logger = Logger.getLogger(Weka.class.getName());
            logger.log(Level.INFO, e.getMessage());
        }
    }



    public void walkforwardStep(String trainArffPathname, String testArffPathname, int trainingIndex) throws Exception{


        // Crea le istanze non filtrate
        DataSource source1 = new DataSource(trainArffPathname);
        Instances trainingNoFilter = source1.getDataSet();
        DataSource source2 = new DataSource(testArffPathname);
        Instances testingNoFilter = source2.getDataSet();
        int numAttrNoFilter = trainingNoFilter.numAttributes();
        trainingNoFilter.setClassIndex(numAttrNoFilter - 1);
        testingNoFilter.setClassIndex(numAttrNoFilter - 1);

        // se non ci sono classi buggy nel training set passo alla prossima iterazione
        if(countClassInstances(trainingNoFilter, numAttrNoFilter)[0] == 0)
            return;
        List<Technique> none = new ArrayList<>();
        none.add(Technique.NONE);
        List<Technique> samplingOnly = new ArrayList<>();
        samplingOnly.add(Technique.UNDERSAMPLING);
        List<Technique> oversamplingOnly = new ArrayList<>();
        oversamplingOnly.add(Technique.OVERSAMPLING);
        List<Technique> sensitivityOnly = new ArrayList<>();
        sensitivityOnly.add(Technique.SENSITIVETHRESHOLD);
        List<Technique> featureAndsensitivity = new ArrayList<>();
        featureAndsensitivity.add(Technique.SENSITIVELEARNING);
        featureAndsensitivity.add(Technique.FEATURESELECTION);
        List<Technique> featureAndUnderSampling = new ArrayList<>();
        featureAndUnderSampling.add(Technique.UNDERSAMPLING);
        featureAndUnderSampling.add(Technique.FEATURESELECTION);
        List<Technique> featureAndOverSampling = new ArrayList<>();
        featureAndOverSampling.add(Technique.OVERSAMPLING);
        featureAndOverSampling.add(Technique.FEATURESELECTION);

        //IBK
        String ibk = "IBk";
        build(ibk, none, trainingNoFilter, testingNoFilter, trainingIndex);
        build(ibk, samplingOnly, trainingNoFilter, testingNoFilter, trainingIndex);
        build(ibk, oversamplingOnly, trainingNoFilter, testingNoFilter, trainingIndex);
        build(ibk, sensitivityOnly, trainingNoFilter, testingNoFilter, trainingIndex);
        build(ibk, featureAndOverSampling, trainingNoFilter, testingNoFilter, trainingIndex);


        //Random Forest
        String rf = "RandomForest";
        build(rf, none, trainingNoFilter, testingNoFilter, trainingIndex);
        build(rf, samplingOnly, trainingNoFilter, testingNoFilter, trainingIndex);
        build(rf, oversamplingOnly, trainingNoFilter, testingNoFilter, trainingIndex);
        build(rf, sensitivityOnly, trainingNoFilter, testingNoFilter, trainingIndex);
        build(rf, featureAndUnderSampling, trainingNoFilter, testingNoFilter, trainingIndex);

        //Naive Bayes
        String nb = "NaiveBayes";
        build(nb, none, trainingNoFilter, testingNoFilter, trainingIndex);
        build(nb, samplingOnly, trainingNoFilter, testingNoFilter, trainingIndex);
        build(nb, oversamplingOnly, trainingNoFilter, testingNoFilter, trainingIndex);
        build(nb, sensitivityOnly, trainingNoFilter, testingNoFilter, trainingIndex);
        build(nb, featureAndsensitivity, trainingNoFilter, testingNoFilter, trainingIndex);


    }


    private boolean techniqueContains(Technique t, List<Technique> techniqueList){
        boolean contains=false;

        for(Technique elem: techniqueList){
            if(elem.equals(t)){
                contains = true;
                break;
            }
        }

        return contains;
    }



    private void build(String classifierString, List<Technique> technique, Instances trainingNoFilter, Instances testingNoFilter, int trainingIndex) throws Exception {

        AttributeSelection filter;
        CfsSubsetEval eval;
        BestFirst search;
        Instances testing = testingNoFilter;
        Instances training = trainingNoFilter;
        AbstractClassifier classifier;

        if(techniqueContains(Technique.FEATURESELECTION, technique)) {
            // STEP 1 >> creare il filtro se necessario (e impostarne l'evaluator e il search algorithm)
            filter = new AttributeSelection();
            eval = new CfsSubsetEval();
            search = new BestFirst();
            filter.setEvaluator(eval);
            filter.setSearch(search);
            filter.setInputFormat(trainingNoFilter);
            // STEP 2 >> crea le istanze filtrate
            Instances trainingFiltered = Filter.useFilter(trainingNoFilter, filter);
            Instances testingFiltered = Filter.useFilter(testingNoFilter, filter);
            int numAttrFiltered = trainingFiltered.numAttributes();
            trainingFiltered.setClassIndex(numAttrFiltered - 1);
            testingFiltered.setClassIndex(numAttrFiltered - 1);

            //imposto la valutazione su questi dataset appena costruiti
            training = trainingFiltered;
            testing = testingFiltered;


        }

        // STEP 3 >> costruisci i classificatori in base alla stringa in input
        if(classifierString.equalsIgnoreCase("RandomForest"))
            classifier = new RandomForest();
        else if(classifierString.equalsIgnoreCase("NaiveBayes"))
            classifier = new NaiveBayes();
        else
            classifier = new IBk();


        if(techniqueContains(Technique.UNDERSAMPLING, technique) || techniqueContains(Technique.OVERSAMPLING, technique)) {


            // classificatore che applica il filtro di sampling
            FilteredClassifier fc = new FilteredClassifier();
            if(technique.contains(Technique.UNDERSAMPLING)) {
                SpreadSubsample spreadSubsample = new SpreadSubsample();
                // random subsample del dataset
                String[] opts = new String[]{"-M", "1.0"};
                spreadSubsample.setOptions(opts);
                spreadSubsample.setInputFormat(training);

                //crea il nuovo training con il filtro
                Instances trainingSampled = Filter.useFilter(training, spreadSubsample);
                training = trainingSampled;
            }
            else if(technique.contains(Technique.OVERSAMPLING)){

                // random subsample usando rimpiazzamento o no
                // -Z taglia del dataset in output (espressa in percentuale del dataset in input)
                Resample resample = new Resample();
                int attribNumber = training.numAttributes();
                double majorityIntances = training.attributeStats(attribNumber-1).nominalCounts[1];
                String outputSizePerc = Double.toString(2*100*(majorityIntances/training.size()));
                String[] opts = new String[]{"-B", "1.0", "-Z", outputSizePerc};

                resample.setOptions(opts);
                resample.setInputFormat(training);

                Instances trainingSampled = Filter.useFilter(training, resample);
                training = trainingSampled;

            }

            fc.setClassifier(classifier);
            classifier = fc;


        }

        classifier.buildClassifier(training);
        Evaluation evaluation = new Evaluation(testing);

        if(techniqueContains(Technique.SENSITIVELEARNING, technique)||techniqueContains(Technique.SENSITIVETHRESHOLD, technique)){
            CostMatrix cm = new CostMatrix(2);
            cm.setCell(0,0,0.0);
            cm.setCell(0,1,10.0);
            cm.setCell(1,0,1.0);
            cm.setCell(1,1,0.0);
            CostSensitiveClassifier costSensitiveClassifier = new CostSensitiveClassifier();
            costSensitiveClassifier.setClassifier(classifier);
            costSensitiveClassifier.setCostMatrix(cm);
            boolean minimize = false;
            if(techniqueContains(Technique.SENSITIVETHRESHOLD, technique))
                minimize = true;
            costSensitiveClassifier.setMinimizeExpectedCost(minimize);
            costSensitiveClassifier.buildClassifier(training);
            classifier = costSensitiveClassifier;
            Evaluation sensitiveEval = new Evaluation(training, cm);
            evaluation = sensitiveEval;
        }


        // STEP 4 >> valuta i classificatori sui dataset
        evaluation.evaluateModel(classifier, testing);


        // STEP 5 >> calcola quanti positivi e quanti negativi ci sono
        int buggyCountTraining = countClassInstances(training, training.numAttributes())[0];
        int noBuggyCountTraining = countClassInstances(training, training.numAttributes())[1];
        int buggyCountTesting = countClassInstances(testing, testing.numAttributes())[0];
        int noBuggyCountTesting = countClassInstances(testing, testing.numAttributes())[1];
        int[] count = {buggyCountTraining,noBuggyCountTraining, buggyCountTesting, noBuggyCountTesting};


        // step 6 >> Scrivi i risultati su excel
        addRow(trainingIndex, evaluation, classifierString, count, technique);

    }

    private int[] countClassInstances(Instances training, int numAttrNoFilter){
        AttributeStats stats = training.attributeStats(numAttrNoFilter-1);
        return stats.nominalCounts;
    }

    private List<Double> getMetrics(int trainingIndex, int[] bugginess, Evaluation evaluation){

        List<Double> metrics = new ArrayList<>();
        int trainingCount = trainingIndex+1;    // incremento necessario perchè trainingindex inizia da 0

        // %training
        double percTraining = (double) trainingCount/(trainingCount+1);
        metrics.add(percTraining);

        //%defective in training
        double percBuggyTraining = (double) bugginess[0]/(bugginess[0]+bugginess[1]);
        metrics.add(percBuggyTraining);

        //%defective in training
        double percBuggyTesting = (double) bugginess[2]/(bugginess[2]+bugginess[3]);
        metrics.add(percBuggyTesting);

        //truePositive
        double truePositive = evaluation.numTruePositives(0);
        metrics.add(truePositive);

        //falsePositive
        double falsePositive = evaluation.numFalsePositives(0);
        metrics.add(falsePositive);

        //trueNegative
        double trueNegative = evaluation.numTrueNegatives(0);
        metrics.add(trueNegative);

        //falseNegative
        double falseNegative = evaluation.numFalseNegatives(0);
        metrics.add(falseNegative);

        return metrics;
    }

}
