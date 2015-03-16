package com.amobi.toanvq.sql;


import com.amobi.toanvq.utils.MatrixUtilities;
import com.amobi.toanvq.utils.NumberUtils;
import org.ojalgo.matrix.BasicMatrix;
import org.ojalgo.matrix.PrimitiveMatrix;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdReader {

    public BasicMatrix.Factory<PrimitiveMatrix> factory= PrimitiveMatrix.FACTORY;

    public static final String AD_ID = "id";
    public static final String AD_AREA = "adv_area";
    public static final String AD_NAME = "name";
    public static final String AD_CATEGORY = "adv_category";
    public static final String AD_SIZE = "adv_size";
    public static final String AD_DEVICE = "device";
    public static final String AD_TYPE = "adv_type";
    public static final String AD_GENDER = "gender";
    public static final String AD_AUDIENCE = "adv_audience";
    public static final String AD_SCREEN_SIZE = "screensize";

    public Double coefficientCategory;
    public Double coefficientType;
    public Double coefficientGender;
    public Double coefficientAudiences;
    public Double coefficientScreenSize;
    public Double coefficientDevice;
    public Double coefficientArea;

    public final Double MAX_AD_SIZE = Double.valueOf(69);
    public final Double MAX_AD_TYPE = Double.valueOf(5);
    public final Double MAX_AD_SCREEN_SIZE = Double.valueOf(3);


    public List<Integer> idList;


    private static Integer[] arrayArea = {
            1, 4, 5
    };

    private static Integer[] arrayGender = {
            0, 1
    };


    private static Integer[] arrayAudiences = {
            1, 2, 3, 4, 5, 6, 7, 8
    };

    private static Integer[] arrayDevice = {
            1, 2
    };

    private static List<Integer> listArea = Arrays.asList(arrayArea);
    private static List<Integer> listCategory = CategoryReader.getListCategory();
    private static List<Integer> listDevice = Arrays.asList(arrayDevice);
    private static List<Integer> listGender = Arrays.asList(arrayGender);
    private static List<Integer> listAudiences = Arrays.asList(arrayAudiences);

    public AdReader() {
        readConfiguration();
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

            coefficientCategory = Double.valueOf(Integer.parseInt(
                    coefficient.getElementsByTagName("category").item(0).getTextContent().trim()
            ));


            coefficientType = Double.valueOf(Integer.parseInt(
                    coefficient.getElementsByTagName("type").item(0).getTextContent().trim()
            ));


            coefficientAudiences = Double.valueOf(Integer.parseInt(
                    coefficient.getElementsByTagName("audiences").item(0).getTextContent().trim()
            ));

            coefficientGender = Double.valueOf(Integer.parseInt(
                    coefficient.getElementsByTagName("gender").item(0).getTextContent().trim()
            ));

            coefficientArea = Double.valueOf(Integer.parseInt(
                    coefficient.getElementsByTagName("area").item(0).getTextContent().trim()
            ));

            coefficientScreenSize = Double.valueOf(Integer.parseInt(
                    coefficient.getElementsByTagName("screensize").item(0).getTextContent().trim()
            ));

            coefficientDevice=Double.valueOf(Integer.parseInt(
                    coefficient.getElementsByTagName("device").item(0).getTextContent().trim()
            ));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BasicMatrix<Double> read() {
        try {
            Class.forName(Config.DB_DRIVER);
            Connection connection = DriverManager.getConnection(Config.DB_URL, Config.DB_USER, Config.DB_PASSWORD);

            String sql = "SELECT * FROM advertistment";
            ResultSet resultSet = connection.createStatement().executeQuery(sql);
            
            
            
            List<List<Double>> matrixList=new ArrayList<List<Double>>();
                    
            idList = new ArrayList<Integer>();

            int indexColumns;
            while (resultSet.next()) {
                indexColumns = 0;
                idList.add(resultSet.getInt(AD_ID));
                List<Double> list = new ArrayList<Double>();


                // analyze area
                String areaRaw = resultSet.getString(AD_AREA);
                List<Double> listArea = analyzeArea(areaRaw, coefficientArea);
                NumberUtils.addToList(listArea, list);

                indexColumns = indexColumns + listArea.size();


                // analyze categories
                String categoryRaw = resultSet.getString(AD_CATEGORY);
                List<Double> listCategory = analyzeCategories(categoryRaw, coefficientCategory);
                NumberUtils.addToList(listCategory, list);
                indexColumns = indexColumns + listCategory.size();

                // Device
                String deviceRaw = resultSet.getString(AD_DEVICE);
                List<Double> listDevice = analyzeDevice(deviceRaw, coefficientDevice);
                NumberUtils.addToList(listDevice, list);
                indexColumns = indexColumns + listDevice.size();

                // AD_TYPE
                list = NumberUtils.addZero(list, 1);
                int typeRaw = resultSet.getInt(AD_TYPE);
                list.set(indexColumns, Double.valueOf(MAX_AD_TYPE - typeRaw)/(Double.valueOf(MAX_AD_TYPE))*coefficientType);
                indexColumns = indexColumns + 1;

                // AD_GENDER
                String genderRaw = resultSet.getString(AD_GENDER);
                List<Double> listGender = analyzeGender(genderRaw, coefficientGender);
                NumberUtils.addToList(listGender, list);
                indexColumns = indexColumns + 1;

                // AD_SIZE
                list = NumberUtils.addZero(list, 1);
                int sizeRaw = resultSet.getInt(AD_SIZE);
                list.set(indexColumns, Double.valueOf(MAX_AD_SIZE - sizeRaw)/(Double.valueOf(MAX_AD_SIZE)));
                indexColumns = indexColumns + 1;

                // ADV_AUDIENCES

                String audienceRaw = resultSet.getString(AD_AUDIENCE);
                List<Double> listAudiences = analyzeAudiences(audienceRaw, coefficientAudiences);
                NumberUtils.addToList(listAudiences, list);
                indexColumns = indexColumns + listAudiences.size();


                // ADV_SCREEN_SIZE

                list = NumberUtils.addZero(list, 1);
                int screenSizeRaw = resultSet.getInt(AD_SCREEN_SIZE);
                list.set(indexColumns, Double.valueOf(MAX_AD_SCREEN_SIZE - screenSizeRaw)/(MAX_AD_SCREEN_SIZE)*coefficientScreenSize);
                
                matrixList.add(list);
            }

            double arrayMatrix[][]=new double[matrixList.size()][matrixList.get(0).size()];
            
            for (int i=0; i<matrixList.size(); i++){
                arrayMatrix[i]=NumberUtils.convertListToArray(matrixList.get(i));
            }
            
            BasicMatrix matrix=factory.rows(arrayMatrix);
            connection.close();
            return matrix;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<Double> analyzeArea(String areaRaw, Double coeff) {
        List<Double> list = new ArrayList<Double>();
        list = NumberUtils.addZero(list, 3);

        if (areaRaw == null) {
            areaRaw = "1 4 5";
        } else {
            areaRaw = areaRaw.replace(",", " ").trim();
        }

        if (areaRaw.isEmpty()) {
            areaRaw = "1 4 5";
        }

        String[] area = areaRaw.split(" ");
        Double areaValue = Double.valueOf(1)/(area.length);
        for (String temp : area) {
            int position = listArea.indexOf(Integer.parseInt(temp));
            list.set(position, areaValue*coeff);
        }

        return list;
    }

    public static List<Double> analyzeCategories(String categoryRaw, Double coeff) {
        List<Double> list = new ArrayList<Double>();
        list = NumberUtils.addZero(list, listCategory.size());

        if (categoryRaw.isEmpty()) {
            for (int i = 0; i < listCategory.size(); i++) {
                Double value = Double.valueOf(1)/(Double.valueOf(listCategory.size()))*(coeff);
                list.set(i, value);
            }
        } else {
            String category[] = categoryRaw.split(" ");
            try {
                for (String value : category) {
                    int position = listCategory.indexOf(Integer.parseInt(value));
                    list.set(position, Double.valueOf(1)/(Double.valueOf(category.length))*(coeff));
                }
            } catch (NumberFormatException e) {
                for (int i = 0; i < listCategory.size(); i++) {
                    list.set(i, Double.valueOf(1)/(Double.valueOf(listCategory.size()))*(coeff));
                }
            }
        }

        return list;
    }

    public static List<Double> analyzeDevice(String deviceRaw, Double coeff) {
        List<Double> list = new ArrayList<Double>();
        list = NumberUtils.addZero(list, listDevice.size());
        if (deviceRaw.isEmpty()) {
            list.set(0, Double.valueOf(0.5)*(coeff));
            list.set(1, Double.valueOf(0.5)*(coeff));
        } else {
            deviceRaw = deviceRaw.replace(",", " ");
            String[] device = deviceRaw.split(" ");
            if (device.length == 2) {
                list.set(0, Double.valueOf(0.5)*(coeff));
                list.set(1, Double.valueOf(0.5)*(coeff));
            } else if (device.length == 1) {
                int position = listDevice.indexOf(Integer.parseInt(device[0]));
                list.set(position, Double.valueOf(coeff));
            }
        }

        return list;
    }

    public static List<Double> analyzeAudiences(String audiencesRaw, Double coeff) {

        List<Double> list = new ArrayList<Double>();
        list = NumberUtils.addZero(list, listAudiences.size());

        if (audiencesRaw == null) {
            // if null
            for (int i = 0; i < listAudiences.size(); i++) {
                list.set(i, Double.valueOf(1)/(listAudiences.size())*(coeff));
            }

        } else if (audiencesRaw.isEmpty()) {

            // if empty
            for (int i = 0; i < listAudiences.size(); i++) {
                list.set(i, Double.valueOf(1)/(listAudiences.size())*(coeff));
            }

        } else {


            audiencesRaw = audiencesRaw.replaceAll(",", " ").trim();
            String[] audience = audiencesRaw.split(" ");

            Double value = Double.valueOf(1)/(audience.length);

            boolean check=false;

            for (String au : audience) {

                try {

                    int temp = Integer.parseInt(au);

                    if (listAudiences.contains(temp)){
                        list.set(temp - 1, value*(coeff));
                        check=true;
                    }

                } catch (NumberFormatException e) {
                    System.out.println(au);
                    e.printStackTrace();
                }

            }

            if (!check){
                for (int i = 0; i < listAudiences.size(); i++) {
                    list.set(i, Double.valueOf(1)/(listAudiences.size())*(coeff));
                }
            }

        }

        return list;
    }

    public static List<Double> analyzeGender(String genderRaw, Double coeff) {
        List<Double> list = new ArrayList<Double>();
        list = NumberUtils.addZero(list, 1);
        if (genderRaw == null) {
            list.set(0, Double.valueOf(0.5));
        } else if (genderRaw.isEmpty()) {
            list.set(0, Double.valueOf(0.5));
        } else {
            genderRaw = genderRaw.replace(",", " ").trim();
            String[] gender = genderRaw.split(" ");
            if (gender.length == 2) {
                list.set(0, Double.valueOf(0.5));
            } else if (gender.length == 1) {
                int position = listGender.indexOf(Integer.parseInt(gender[0]));
                list.set(0, Double.valueOf(position));
            }
        }
        list.set(0, list.get(0)*(coeff));

        return list;
    }


    public static void main(String args[]) {
        AdReader adReader=new AdReader();
        adReader.factory.makeEye(1000, 1000);
        BasicMatrix matrix=adReader.read();


        MatrixUtilities.printMatrix(matrix);
    }
}

