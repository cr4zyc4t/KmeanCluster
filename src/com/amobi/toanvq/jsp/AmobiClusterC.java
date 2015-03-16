package com.amobi.toanvq.jsp;


import com.amobi.toanvq.sql.*;
import com.amobi.toanvq.utils.CMeans;
import com.amobi.toanvq.utils.MatrixUtilities;
import com.amobi.toanvq.utils.NumberUtils;
import org.ojalgo.matrix.BasicMatrix;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public class AmobiClusterC {

    private AdReader adReader;
    private AppReader appReader;
    private LinkReader linkReader;

    private BasicMatrix adMatrix;
    private BasicMatrix appMatrix;
    private BasicMatrix adAppMatrix;
    private BasicMatrix appAdMatrix;

    private double coeff=2;

    private BasicMatrix AGU, AGUA;
    private BasicMatrix UGA, UGAU;

    private BasicMatrix GU, GA;

    private BasicMatrix GAGU;
    private BasicMatrix GUGA;

    private int numAdGroup, numAppGroup;
    private double weightClickForApp=5;
    private double weightClickForAd=5;

    private int kTimes = 100;
    private int amobiTimes=200;

    public AmobiClusterC() {

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
            Node conAlgorithm = doc.getElementsByTagName("algorithm").item(0);

            Element element = (Element) conAlgorithm;

            numAdGroup=Integer.parseInt(
                    element.getElementsByTagName("num-ad-cluster").item(0).getTextContent().trim()
            );
            numAppGroup=Integer.parseInt(
                    element.getElementsByTagName("num-app-cluster").item(0).getTextContent().trim()
            );

            kTimes = Integer.parseInt(
                    element.getElementsByTagName("kmeans-times").item(0).getTextContent().trim()
            );


            amobiTimes = Integer.parseInt(
                    element.getElementsByTagName("amobi-times").item(0).getTextContent().trim()
            );

            Element advertisement = (Element) doc.getElementsByTagName("advertisement").item(0);
            Element application=(Element) doc.getElementsByTagName("application").item(0);


            Element coefficient = (Element) advertisement.getElementsByTagName("coefficient").item(0);
            this.weightClickForAd=Double.valueOf(Integer.parseInt(
                    coefficient.getElementsByTagName("weight-click").item(0).getTextContent().trim()
            ));

            coefficient = (Element) application.getElementsByTagName("coefficient").item(0);
            this.weightClickForApp=Double.valueOf(Integer.parseInt(
                    coefficient.getElementsByTagName("weight-click").item(0).getTextContent().trim()
            ));


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

        System.out.println("Reading Advertisement Matrix.....");
        adMatrix = adReader.read();
        adMatrix = MatrixUtilities.normalizeMatrix(adMatrix);

        System.out.println("Reading Application Matrix.....");
        appMatrix = appReader.read();
        appMatrix = MatrixUtilities.normalizeMatrix(appMatrix);

        linkReader = new LinkReader(
                adReader.idList,
                appReader.idList,
                new LinkConverter().getLinkIDAdvIDMap()
        );

        System.out.println("Reading Adv-App Relation Matrix...");
        adAppMatrix = linkReader.read();

        appAdMatrix = adAppMatrix.transpose();

        adAppMatrix = MatrixUtilities.normalizeMatrix(adAppMatrix);
        appAdMatrix = MatrixUtilities.normalizeMatrix(appAdMatrix);

        System.out.println(adAppMatrix.toString());


    }


    /**
     * Begin implement
     */
    public void executeICCA() {
        System.out.println("Cluster Adv Matrix to GA...");
        GA=new CMeans(adMatrix, numAdGroup, coeff).execute(this.kTimes);


        System.out.println("Cluster App Matrix to GU...");
        GU=new CMeans(appMatrix, numAppGroup, coeff).execute(this.kTimes);

        System.out.println("Progress Cluster AGUA and UGAU start....");
        for (int i = 0; i < amobiTimes; i++) {

            System.out.println("Cluster AGUA to update GA times " + i + "...");

            AGU = MatrixUtilities.normalizeMatrix(adAppMatrix.multiplyRight(GU));
            AGUA = MatrixUtilities.normalizeMatrix(MatrixUtilities.combineMatrix(AGU, adMatrix, this.weightClickForAd, 1 ));


            GA = new CMeans(AGUA, numAdGroup, coeff).execute(this.kTimes);

            UGA = MatrixUtilities.normalizeMatrix(appAdMatrix.multiplyRight(GA));
            UGAU = MatrixUtilities.normalizeMatrix(MatrixUtilities.combineMatrix(UGA, appMatrix, this.weightClickForApp, 1));

            GU = new CMeans(UGAU, numAppGroup, coeff).execute(this.kTimes);
        }

        MatrixUtilities.toFile("GA"+ numAdGroup +"_"+ numAppGroup +"CMeans.txt", GA);
        MatrixUtilities.toFile("GU"+ numAdGroup +"_"+ numAppGroup +"CMeans.txt", GU);
        MatrixUtilities.toFile("AGU"+ numAdGroup +"_"+ numAppGroup +"CMeans.txt", AGU);
        MatrixUtilities.toFile("UGA"+ numAdGroup +"_"+ numAppGroup +"CMeans.txt", UGA);

        GAGU = GA.transpose().multiplyRight(AGU);
        GUGA = GU.transpose().multiplyRight(UGA);

        Double divergenceAdv = MatrixUtilities.divergenceMatrix(GAGU);
        Double divergenceApp = MatrixUtilities.divergenceMatrix(GUGA);

        System.out.println(divergenceAdv + "   " + divergenceApp + "  " + (divergenceAdv+(divergenceApp)));
        NumberUtils.toFile("KQ"+new Integer(numAdGroup).toString()
                +new Integer(numAppGroup).toString()+"CMeans.txt",divergenceAdv.toString()+" "
                +divergenceApp.toString()+" "
                +divergenceAdv+(divergenceApp).toString());

        NumberUtils.printList("ADV_ID"+ numAdGroup +"_"+ numAppGroup +"CMeans.txt", adReader.idList);
        NumberUtils.printList("APP_ID"+ numAdGroup +"_"+ numAppGroup +"CMeans.txt", appReader.idList);
    }

    public static void main(String args[]){
        new AmobiClusterC().executeICCA();
    }
}
