package com.amobi.toanvq.jsp;


import com.amobi.toanvq.result.PrintCluster;
import com.amobi.toanvq.sql.*;
import com.amobi.toanvq.utils.KMeans;
import com.amobi.toanvq.utils.MatrixUtilities;
import com.amobi.toanvq.utils.NumberUtils;
import org.ojalgo.matrix.BasicMatrix;
import org.ojalgo.matrix.PrimitiveMatrix;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.Arrays;
import java.util.Calendar;

public class ICCC {

    private BasicMatrix.Factory<PrimitiveMatrix> factory= PrimitiveMatrix.FACTORY;

    private AdReader adReader;
    private AppReader appReader;
    private LinkReader linkReader;


    private BasicMatrix adMatrix;
    private BasicMatrix appMatrix;
    private BasicMatrix adAppMatrix;
    private BasicMatrix appAdMatrix;

    private BasicMatrix AGU, AGUA;
    private BasicMatrix UGA, UGAU;

    private BasicMatrix GU, GA;

    private BasicMatrix GAGU;
    private BasicMatrix GUGA;

    private int numAdGroup, numAppGroup;


    private int kTimes = 100;
    private int amobiTimes=200;

    private double weightClickForApp;
    private double weightClickForAd;

    public ICCC() {

        initData();
        initConfiguration();
    }

    /**
     * Read configuration from configuration.xml
     * Parameters to be read: kTimes, amobiTimes
     */
    public void initConfiguration() {
        try {

            // Begin read configuration

            File xmlConfiguration = new File(Config.CONFIGURATION);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            Document doc = dBuilder.parse(xmlConfiguration);

            doc.getDocumentElement().normalize();


            // Begin read configuration
            Element conAlgorithm = (Element) doc.getElementsByTagName("algorithm").item(0);

            Element advertisement = (Element) doc.getElementsByTagName("advertisement").item(0);
            Element application=(Element) doc.getElementsByTagName("application").item(0);


            Element adCoefficient = (Element) advertisement.getElementsByTagName("coefficient").item(0);
            Element appCoefficient = (Element) application.getElementsByTagName("coefficient").item(0);

            numAdGroup=Integer.parseInt(
                    conAlgorithm.getElementsByTagName("num-ad-cluster").item(0).getTextContent().trim()
            );

            numAppGroup=Integer.parseInt(
                    conAlgorithm.getElementsByTagName("num-app-cluster").item(0).getTextContent().trim()
            );

            kTimes = Integer.parseInt(
                    conAlgorithm.getElementsByTagName("kmeans-times").item(0).getTextContent().trim()
            );


            amobiTimes = Integer.parseInt(
                    conAlgorithm.getElementsByTagName("amobi-times").item(0).getTextContent().trim()
            );




            this.weightClickForAd=Double.parseDouble(
                    adCoefficient.getElementsByTagName("weight-click").item(0).getTextContent().trim()
            );

            this.weightClickForApp=Double.parseDouble(
                    appCoefficient.getElementsByTagName("weight-click").item(0).getTextContent().trim()
            );




        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Init all the data
     */
    public void initData(){
        adReader = new AdReader();
        appReader = new AppReader();

        System.out.println("Reading Advertisement BasicMatrix.....");

        adMatrix = adReader.read();
        adMatrix = MatrixUtilities.normalizeMatrix(adMatrix);
        //MatrixUtilities.toFile("AdMatrix.csv", adMatrix);


        System.out.println("Reading Application BasicMatrix.....");
        appMatrix = appReader.read();
        appMatrix = MatrixUtilities.normalizeMatrix(appMatrix);
        //MatrixUtilities.toFile("AppMatrix.csv", appMatrix);

        linkReader = new LinkReader(adReader.idList, appReader.idList, new LinkConverter().getLinkIDAdvIDMap());

        System.out.println("Reading Adv-App Relation BasicMatrix...");
        adAppMatrix = linkReader.read();
        appAdMatrix = adAppMatrix.transpose();

        adAppMatrix = MatrixUtilities.normalizeMatrix(adAppMatrix);
        appAdMatrix = MatrixUtilities.normalizeMatrix(appAdMatrix);
    }


    /**
     * Begin implement
     */
    public void executeICCC() {
        System.out.println("Cluster Adv BasicMatrix to GA...");
        GA = new KMeans(numAdGroup, adMatrix).execute(kTimes);
        System.out.println (GA.toString());

        System.out.println("Cluster App BasicMatrix to GU...");
        GU = new KMeans(numAppGroup, appMatrix).execute(kTimes);
        System.out.println (GU.toString());

        for (int i = 0; i < amobiTimes; i++) {
            System.out.println ("Times "+i);

            AGU = MatrixUtilities.normalizeMatrix(computeRelation(adMatrix, GU, appAdMatrix));

            AGUA = MatrixUtilities.normalizeMatrix(MatrixUtilities.combineMatrix(AGU, adMatrix, this.weightClickForAd, 1));
            GA = new KMeans(numAdGroup, AGUA).execute(kTimes);

            UGA = MatrixUtilities.normalizeMatrix(computeRelation(appMatrix, GA, adAppMatrix));
            UGAU = MatrixUtilities.normalizeMatrix(MatrixUtilities.combineMatrix(UGA, appMatrix,this.weightClickForApp, 1));
            GU = new KMeans(numAppGroup, UGAU).execute(kTimes);
        }


        // Create directory
        Calendar calendar=Calendar.getInstance();

        int day=calendar.get(Calendar.DATE);
        int month=calendar.get(Calendar.MONTH);
        int year=calendar.get(Calendar.YEAR);
        int hour=calendar.get(Calendar.HOUR_OF_DAY);

        String directory=hour+"--"+day+"-"+month+"-"+year;

        File file=new File(directory);

        if (!file.exists()){
            file.mkdir();
        }



        // print all file to directory
        String clusterAdFilename=directory+"/GA"+ numAdGroup +"_"+ numAppGroup +"KMeans.txt";
        String clusterAppFilename=directory+"/GU"+ numAdGroup +"_"+ numAppGroup +"KMeans.txt";

        MatrixUtilities.toFile(clusterAdFilename, GA);
        MatrixUtilities.toFile(clusterAppFilename, GU);

        MatrixUtilities.toFile(directory+"/AGU"+ numAdGroup +"_"+ numAppGroup +"KMeans.txt", AGU);
        MatrixUtilities.toFile(directory+"/"+"UGA"+ numAdGroup +"_"+ numAppGroup +"KMeans.txt", UGA);

        GAGU = computeGAGU();
        MatrixUtilities.normalizeMatrix(GAGU);
        GUGA = GAGU.transpose();

        Double divergenceAdv = MatrixUtilities.divergenceMatrix(GAGU);
        Double divergenceApp = MatrixUtilities.divergenceMatrix(GUGA);

        MatrixUtilities.printMatrix(GAGU);
        MatrixUtilities.printMatrix(GUGA);


        System.out.println(divergenceAdv + "   " + divergenceApp + "  " + (divergenceAdv+(divergenceApp)));
        NumberUtils.toFile(
                directory+"/"+"KQ"+numAdGroup+numAppGroup+"KMeans.txt",
                divergenceAdv.toString()+" "+divergenceApp.toString()+" "+divergenceAdv+(divergenceApp).toString()
        );

        String idAdFileName=directory+"/"+"ADV_ID"+ numAdGroup +"_"+ numAppGroup +"KMeans.txt";
        String idAppFileName=directory+"/"+"APP_ID"+ numAdGroup +"_"+ numAppGroup +"KMeans.txt";

        NumberUtils.printList(idAdFileName, adReader.idList);
        NumberUtils.printList(idAppFileName, appReader.idList);

        PrintCluster printCluster=new PrintCluster();

        String adClusterName=directory+"/ad_cluster"+numAdGroup+numAppGroup+"KMeans.csv";
        String appClusterName=directory+"/app_cluster"+numAdGroup+numAppGroup+"KMeans.csv";

        printCluster.printAdCluster(clusterAdFilename, idAdFileName, adClusterName, numAdGroup);
        printCluster.printAppCluster(clusterAppFilename, idAppFileName, appClusterName, numAppGroup);
    }


    /**
     * Supported function to calculate UGA
     * @param U
     * @param GA
     * @param link: Link Matrix AU
     * @return
     */
    public BasicMatrix computeRelation(BasicMatrix U, BasicMatrix GA, BasicMatrix link) {
        int numRows = (int) U.countRows();
        int numColumns = (int) GA.countColumns();

        double w[][] = new double[numRows][numColumns];
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numColumns; j++) {
                // UGA[i, j]= tong tat ca cac L[k, i] sao cho GA[k][j]=1;

                double value=0;

                for (int k = 0; k < GA.countRows(); k++) {
                    if (GA.get(k, j).doubleValue()== 1) {
                        value = value+(link.get(k, i).doubleValue());
                    }
                }
                w[i][j] = value;
            }
        }
        return factory.rows(w);
    }


    /**
     * Support function to calculate GAGU
     * @return
     */
    private BasicMatrix computeGAGU() {
        int numRows = numAdGroup;
        int numColumns = numAppGroup;

        double w[][] = new double[numRows][numColumns];

        for (int i = 0; i < numRows; i++) {
            Arrays.fill(w[i], 0.0);
        }

        for (int i = 0; i < GA.countRows(); i++) {
            for (int j = 0; j < GU.countRows(); j++) {

                // xac dinh xem i thuoc nhom Ad nao
                int groupI = 0;
                for (int m = 0; m < GA.countColumns(); m++) {
                    if (GA.get(i, m).doubleValue()== 1) {
                        groupI = m;
                    }
                }
                // xac dinh xem j thuoc nhom app nao
                int groupJ = 0;
                for (int m = 0; m < GU.countColumns(); m++) {
                    if (GU.get(j, m).doubleValue() == 1) {
                        groupJ = m;
                    }
                }
                w[groupI][groupJ] = w[groupI][groupJ]+(adAppMatrix.get(i, j).doubleValue());
            }
        }

        return factory.rows(w);
    }

    public static void main(String args[]){
        Config.setCONFIGURATION("configuration.xml");
        new AmobiClusterK().executeICCA();
    }
}
