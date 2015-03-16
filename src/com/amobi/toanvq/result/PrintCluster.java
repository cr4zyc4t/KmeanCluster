package com.amobi.toanvq.result;


import com.amobi.toanvq.sql.AdReader;
import com.amobi.toanvq.sql.AppReader;
import com.amobi.toanvq.sql.Config;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;


public class PrintCluster {
    public static double CATEGORY_COEFF = 5;
    public static double ADV_TYPE_COEFF = 3;
    public static double GENDER_TYPE_COEFF = 2;
    public static double ADV_AUDIENCE_COEFF = 5;
    public static double AD_SCREEN_SIZE_COEFF;
    public static double AD_DEVICE_COEFF = 1;
    public static double AD_AREA_COEFF = 1;


    public static double APP_DEVICE_COEFF = 1;
    public static double APP_CATEGORY_COEFF = 1;
    public static double APP_AUDIENCES_COEFF = 1;
    public static double APP_GENDER_COEFF = 1;
    public static double APP_SMART_STATUS_COEFF = 1;
    public static double APP_RATE_COEFF = 1;
    public static double APP_DOWNLOAD_COEFF = 1;
    public static double APP_LIKE_COEFF = 1;


    public static final String GAP = ",";

    public List<Integer> getIdList(String input) {
        try {
            FileInputStream fileInputStream = new FileInputStream(input);
            Scanner scanner = new Scanner(fileInputStream);

            int count = 0;
            List<Integer> list = new ArrayList<Integer>();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String row[] = line.trim().split(" ");
                list.add(Integer.parseInt(row[1]));
            }
            return list;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void readConfiguration() {
        try {
            File xmlConfiguration = new File(Config.CONFIGURATION);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            Document doc = dBuilder.parse(xmlConfiguration);

            doc.getDocumentElement().normalize();


            // Read Advertisement

            Element advertisement = (Element) doc.getElementsByTagName("advertisement").item(0);


            Element coefficient = (Element) advertisement.getElementsByTagName("coefficient").item(0);

            CATEGORY_COEFF = Integer.parseInt(
                    coefficient.getElementsByTagName("category").item(0).getTextContent().trim()
            );


            ADV_TYPE_COEFF = Integer.parseInt(
                    coefficient.getElementsByTagName("type").item(0).getTextContent().trim()
            );


            ADV_AUDIENCE_COEFF = Integer.parseInt(
                    coefficient.getElementsByTagName("audiences").item(0).getTextContent().trim()
            );

            GENDER_TYPE_COEFF = Integer.parseInt(
                    coefficient.getElementsByTagName("gender").item(0).getTextContent().trim()
            );

            AD_AREA_COEFF = Integer.parseInt(
                    coefficient.getElementsByTagName("area").item(0).getTextContent().trim()
            );

            AD_SCREEN_SIZE_COEFF = Integer.parseInt(
                    coefficient.getElementsByTagName("screensize").item(0).getTextContent().trim()
            );

            // Read application

            Element application = (Element) doc.getElementsByTagName("application").item(0);

            APP_CATEGORY_COEFF = Double.parseDouble(
                    application.getElementsByTagName("category").item(0).getTextContent().trim()
            );

            APP_AUDIENCES_COEFF = Double.parseDouble(
                    application.getElementsByTagName("audiences").item(0).getTextContent().trim()
            );

            APP_DOWNLOAD_COEFF = Double.parseDouble(
                    application.getElementsByTagName("download").item(0).getTextContent().trim()
            );
            APP_GENDER_COEFF = Double.parseDouble(
                    application.getElementsByTagName("gender").item(0).getTextContent().trim()
            );
            APP_LIKE_COEFF = Double.parseDouble(
                    application.getElementsByTagName("like").item(0).getTextContent().trim()
            );

            APP_RATE_COEFF = Double.parseDouble(
                    application.getElementsByTagName("rate").item(0).getTextContent().trim()
            );
            APP_SMART_STATUS_COEFF = Double.parseDouble(
                    application.getElementsByTagName("smartstatus").item(0).getTextContent().trim()
            );

            APP_DEVICE_COEFF = Double.parseDouble(
                    application.getElementsByTagName("device").item(0).getTextContent().trim()
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public List<String> getCodeList(String input) {
        try {
            FileInputStream fileInputStream = new FileInputStream(input);
            Scanner scanner = new Scanner(fileInputStream);

            int count = 0;
            List<String> list = new ArrayList<String>();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String row[] = line.split(" ");
                list.add(row[1].trim());
            }
            return list;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }


    public List<Integer> readClusterFile(String clusterFile) {
        try {
            FileInputStream fileInputStream = new FileInputStream(clusterFile);
            Scanner scanner = new Scanner(fileInputStream);

            int count = 0;

            List<Integer> list = new ArrayList<Integer>();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                line = line.replace(",", " ");
                String row[] = line.trim().split(" ");

                int cluster = 0;
                for (int i = 0; i < row.length; i++) {
                    if (Float.parseFloat(row[i]) == 1) {
                        cluster = i;
                    }
                }
                list.add(cluster);
            }
            return list;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }


    public void printAppCluster(String clusterFile, String indexFiles, String output, int K) {
        try {
            Class.forName(Config.DB_DRIVER);
            Connection connection = DriverManager.getConnection(Config.DB_URL, Config.DB_USER, Config.DB_PASSWORD);


            readConfiguration();

            List<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
            for (int i = 0; i < K; i++) {
                result.add(new ArrayList<String>());
            }

            // init idList
            List<String> codeList = getCodeList(indexFiles);
            List<Integer> clusterList = readClusterFile(clusterFile);

            for (int i = 0; i < clusterList.size(); i++) {
                String widget_code = codeList.get(i);
                int cluster = clusterList.get(i);

                String sql = "SELECT * FROM widget_app WHERE code='" + widget_code.trim() + "'";
                System.out.println(sql);
                ResultSet resultSet = connection.createStatement().executeQuery(sql);

                String detail = "";
                while (resultSet.next()) {

                    List<Double> listAudiences = AppReader.analyzeAudiences(resultSet.getString(AppReader.APP_AUDIENCES), APP_AUDIENCES_COEFF);
                    List<Double> listCategories = AppReader.analyzeCategories(resultSet.getString(AppReader.APP_CATEGORY), APP_CATEGORY_COEFF);
                    List<Double> listGender = AppReader.analyzeGender(resultSet.getString(AppReader.APP_GENDER), APP_GENDER_COEFF);

                    detail = resultSet.getString(AppReader.NAME) + GAP;

                    for (int j = 0; j < listAudiences.size(); j++) {
                        detail = detail + listAudiences.get(j).doubleValue() + GAP;
                    }

                    for (int j = 0; j < listCategories.size(); j++) {
                        detail = detail + listCategories.get(j).doubleValue() + GAP;
                    }
                    for (int j = 0; j < listGender.size(); j++) {
                        detail = detail + listGender.get(j).doubleValue() + GAP;
                    }

                    detail = detail + resultSet.getString(AppReader.CODE) + GAP
                            + resultSet.getString(AppReader.APP_AUDIENCES) + GAP
                            + resultSet.getString(AppReader.APP_DOWNLOAD) + GAP
                            + resultSet.getString(AppReader.APP_GENDER) + GAP
                            + resultSet.getString(AppReader.APP_STATUS) + GAP
                            + resultSet.getString(AppReader.APP_DOWNLOAD) + GAP
                            + resultSet.getString(AppReader.APP_LIKE);
                }
                System.out.println("Cluster: " + cluster);
                result.get(cluster).add(detail);
            }

            connection.close();


            // Print name to file

            try {
                File file = new File(output);

                if (!file.exists()) {
                    file.createNewFile();
                }

                FileWriter fileWriter = new FileWriter(file);

                for (int i = 0; i < result.size(); i++) {
                    fileWriter.write("Cluster " + i + ": \n");
                    for (int j = 0; j < result.get(i).size(); j++) {
                        fileWriter.write(result.get(i).get(j) + "\n");
                    }
                    fileWriter.write("\n");
                }

                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void printAdCluster(String clusterFile, String indexFiles, String output, int K) {
        try {
            Class.forName(Config.DB_DRIVER);
            Connection connection = DriverManager.getConnection(Config.DB_URL, Config.DB_USER, Config.DB_PASSWORD);

            readConfiguration();


            List<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
            for (int i = 0; i < K; i++) {
                result.add(new ArrayList<String>());
            }

            // init idList
            List<Integer> IdList = getIdList(indexFiles);
            List<Integer> clusterList = readClusterFile(clusterFile);

            for (int i = 0; i < clusterList.size(); i++) {
                int id = IdList.get(i);
                int cluster = clusterList.get(i);

                String sql = "SELECT * FROM advertistment WHERE id='" + id + "'";
                System.out.println(sql);
                ResultSet resultSet = connection.createStatement().executeQuery(sql);

                String detail = "";
                while (resultSet.next()) {

                    List<Double> category = AdReader.analyzeCategories(resultSet.getString(AdReader.AD_CATEGORY), CATEGORY_COEFF);
                    List<Double> audiences = AdReader.analyzeAudiences(resultSet.getString(AdReader.AD_AUDIENCE), CATEGORY_COEFF);
                    List<Double> area = AdReader.analyzeArea(resultSet.getString(AdReader.AD_AREA), CATEGORY_COEFF);

                    detail = resultSet.getString(AdReader.AD_NAME) + GAP + resultSet.getString(AdReader.AD_ID) + GAP;

                    for (int j = 0; j < category.size(); j++) {
                        detail = detail + category.get(j).doubleValue() + GAP;
                    }

                    for (int j = 0; j < audiences.size(); j++) {
                        detail = detail + audiences.get(j).doubleValue() + GAP;
                    }

                    for (int j = 0; j < area.size(); j++) {
                        detail = detail + area.get(j).doubleValue() + GAP;
                    }

                    detail = detail + resultSet.getString(AdReader.AD_SIZE) + GAP
                            + resultSet.getString(AdReader.AD_TYPE) + GAP
                            + resultSet.getString(AdReader.AD_GENDER) + GAP
                            + resultSet.getString(AdReader.AD_SCREEN_SIZE) + GAP
                            + resultSet.getString(AdReader.AD_DEVICE);
                }
                System.out.println("Cluster: " + cluster);
                result.get(cluster).add(detail);
            }

            connection.close();


            // Print name to file

            try {
                File file = new File(output);

                if (!file.exists()) {
                    file.createNewFile();
                }

                FileWriter fileWriter = new FileWriter(file);

                for (int i = 0; i < result.size(); i++) {
                    fileWriter.write("Cluster " + i + ": \n");
                    for (int j = 0; j < result.get(i).size(); j++) {
                        fileWriter.write(result.get(i).get(j) + "\n");
                    }
                    fileWriter.write("\n");
                }

                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static void main(String args[]) {
        int i = 4;
        int j = 5;
        System.out.println("Is extracting " + i + " " + j);
        String name = "GU" + i + "_" + j + "KMeans.txt";
        String id = "APP_ID" + i + "_" + j + "KMeans.txt";


        Calendar calendar=Calendar.getInstance();


    }

}
