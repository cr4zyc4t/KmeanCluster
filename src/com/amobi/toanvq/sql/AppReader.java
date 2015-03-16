package com.amobi.toanvq.sql;


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

public class AppReader {

    private BasicMatrix.Factory<PrimitiveMatrix> factory= PrimitiveMatrix.FACTORY;

    public static final String NAME="name";
    public static final String CODE="code";

    public static final String APP_AUDIENCES="audiences";
    public static final int NUMBER_FIELD_AUDIENCES=8;
    public static final String APP_CATEGORY="category";
    //public static final String APP_TYPE="type";
    public static final String APP_STATUS="status";
    public static final int MAX_APP_STATUS=3;
    public static final String APP_GENDER="gender";
    public static final int MAX_APP_GENDER=2;

    public static final String APP_SMART_STATUS="smart_status";

    public static final String APP_RATE="rate";
    public static final int APP_MAX_RATE=5;

    public static final String APP_DOWNLOAD="download";
    public static final int APP_MAX_DOWNLOAD=6784;

    public static final String APP_LIKE="like";
    public static final int APP_MAX_LIKE=24;


    public static long APP_DEVICE_COEFF=1;
    public static long APP_CATEGORY_COEFF=1;
    public static long APP_AUDIENCES_COEFF=1;
    public static long APP_GENDER_COEFF=1;
    public static long APP_SMART_STATUS_COEFF=1;
    public static long APP_RATE_COEFF=1;
    public static long APP_DOWNLOAD_COEFF=1;
    public static long APP_LIKE_COEFF=1;

    private static List<Integer> list_category= CategoryReader.getListCategory();

    private static Integer[] arrayAudiences={
            1, 2, 3, 4, 5, 6, 7, 8
    };

    private static List<Integer> list_audiences= Arrays.asList(arrayAudiences);

    public List<String> idList=new ArrayList<String>();

    public AppReader() {
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

            Element application = (Element) doc.getElementsByTagName("application").item(0);

            APP_CATEGORY_COEFF = Long.parseLong(
                    application.getElementsByTagName("category").item(0).getTextContent().trim()
            );

            APP_AUDIENCES_COEFF=Long.parseLong(
                    application.getElementsByTagName("audiences").item(0).getTextContent().trim()
            );

            APP_DOWNLOAD_COEFF=Long.parseLong(
                    application.getElementsByTagName("download").item(0).getTextContent().trim()
            );
            APP_GENDER_COEFF=Long.parseLong(
                    application.getElementsByTagName("gender").item(0).getTextContent().trim()
            );
            APP_LIKE_COEFF=Long.parseLong(
                    application.getElementsByTagName("like").item(0).getTextContent().trim()
            );

            APP_RATE_COEFF=Long.parseLong(
                    application.getElementsByTagName("rate").item(0).getTextContent().trim()
            );
            APP_SMART_STATUS_COEFF=Long.parseLong(
                    application.getElementsByTagName("smartstatus").item(0).getTextContent().trim()
            );

            APP_DEVICE_COEFF=Long.parseLong(
                    application.getElementsByTagName("device").item(0).getTextContent().trim()
            );


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BasicMatrix read(){

        try{
            Class.forName(Config.DB_DRIVER);
            Connection connection=DriverManager.getConnection(Config.DB_URL, Config.DB_USER, Config.DB_PASSWORD);

            String query="select * from widget_app";
            ResultSet resultSet=connection.createStatement().executeQuery(query);

            List<List<Double>> listMatrix=new ArrayList<List<Double>>();

            while (resultSet.next()){

                // start read vector
                int index_columns=0;

                // init vector
                List<Double> list=new ArrayList<Double>();

                // add index of vector to index list
                idList.add(resultSet.getString(CODE));

                // analyze audiences

                String audiences=resultSet.getString(APP_AUDIENCES);
                List<Double> audiencesResult=analyzeAudiences(audiences, APP_AUDIENCES_COEFF);
                NumberUtils.addToList(audiencesResult, list);

                index_columns=index_columns+NUMBER_FIELD_AUDIENCES;

                // get category
                String categories=resultSet.getString(APP_CATEGORY);
                List<Double> categoriesResult=analyzeCategories(categories, APP_CATEGORY_COEFF);
                NumberUtils.addToList(categoriesResult, list);

                index_columns=index_columns+list_category.size();


                //get Status
                NumberUtils.addZero(list, 1);
                int status=resultSet.getInt(APP_STATUS);
                list.set(index_columns, Double.valueOf(MAX_APP_STATUS - status)/Double.valueOf(MAX_APP_STATUS)*(APP_SMART_STATUS_COEFF));
                index_columns++;

                //get App gender
                List<Double> genderResult=analyzeGender(resultSet.getString(APP_GENDER), APP_GENDER_COEFF);
                NumberUtils.addToList(genderResult, list);
                index_columns++;

                //get App Smart Status
                NumberUtils.addZero(list, 1);
                int appSmartStatus=resultSet.getInt(APP_SMART_STATUS);
                list.set(index_columns, Double.valueOf(appSmartStatus)*(APP_SMART_STATUS_COEFF));
                index_columns++;

                // Get App rate;
                NumberUtils.addZero(list, 1);
                Integer rate=resultSet.getInt(APP_RATE);
                if (rate==null){
                    rate=0;
                }
                list.set(index_columns, Double.valueOf(APP_MAX_RATE - rate)/Double.valueOf(APP_MAX_RATE)*(APP_RATE_COEFF));
                index_columns++;

                // Get App Download
                NumberUtils.addZero(list, 1);
                Integer download=resultSet.getInt(APP_DOWNLOAD);
                if (download==null){
                    download=0;
                }
                list.set(index_columns, Double.valueOf(APP_MAX_DOWNLOAD - download)/Double.valueOf(APP_MAX_DOWNLOAD)*(APP_DOWNLOAD_COEFF));
                index_columns++;

                //get App Like
                NumberUtils.addZero(list, 1);
                Integer like=resultSet.getInt(APP_LIKE);
                if (like==null){
                    like=0;
                }
                list.set(index_columns, Double.valueOf(APP_MAX_LIKE - like)/Double.valueOf(APP_MAX_LIKE)*(APP_LIKE_COEFF));
                index_columns++;

                //
                listMatrix.add(list);
            }

            double arrayMatrix[][]=new double[listMatrix.size()][listMatrix.get(0).size()];

            for (int i=0; i<arrayMatrix.length; i++){
                arrayMatrix[i]= NumberUtils.convertListToArray(listMatrix.get(i));
            }
            connection.close();
            return factory.rows(arrayMatrix);

        } catch (ClassNotFoundException e){
            e.printStackTrace();
            return null;


        } catch (SQLException e){
            e.printStackTrace();
            return null;
        }
    }



    public static List<Double> analyzeAudiences(String audiences, double coeff){
        List<Double> list=new ArrayList<Double>();
        NumberUtils.addZero(list, NUMBER_FIELD_AUDIENCES);

        if (audiences==null){

            // if null
            for (int i=0; i<NUMBER_FIELD_AUDIENCES; i++){
                list.set(i, Double.valueOf(1) / Double.valueOf(NUMBER_FIELD_AUDIENCES) * (coeff));
            }

        } else {

            audiences=audiences.replace(","," ").trim();

            if (audiences.isEmpty()){

                // if empty
                for (int i=0; i<NUMBER_FIELD_AUDIENCES; i++){
                    list.set(i, Double.valueOf(1) / Double.valueOf(NUMBER_FIELD_AUDIENCES) * (coeff));
                }

            } else {

                // not empty

                String[] listAudiences=audiences.split(" ");
                Double value=Double.valueOf(1)/Double.valueOf(listAudiences.length);

                // check if after check valid of element, the array do not contains any thing
                // return true: contains something, false: not contains any thing
                boolean check=false;

                for (String temp:listAudiences){
                    int element=Integer.parseInt(temp);

                    // check if the element of array is valid
                    if (list_audiences.contains(element)){
                        list.set(element-1, value*(coeff));
                        check=true;
                    }
                }


                if (!check){

                    // if array do not contains any valid element
                    for (int i=0; i<NUMBER_FIELD_AUDIENCES; i++){
                        list.set(i, Double.valueOf(1)/Double.valueOf(NUMBER_FIELD_AUDIENCES)*(coeff));
                    }
                }
            }
        }

        //System.out.println (list);
        return list;
    }


    public static List<Double> analyzeCategories(String categories, double coeff){
        List<Double> list=new ArrayList<Double>();
        NumberUtils.addZero(list, list_category.size());
        if (categories==null){

            // if null
            for (int i=0; i<list_category.size(); i++){
                list.set(i, Double.valueOf(1) / Double.valueOf(list_category.size()) * (coeff));
            }
        } else {

            categories=categories.replace(",", " ").trim();
            if (categories.isEmpty()){

                // if empty
                for (int i=0; i<list_category.size(); i++){
                    list.set(i, Double.valueOf(1) / (Double.valueOf(list_category.size()) * (coeff)));
                }
            } else {

                // if not empty
                String[] app_categories=categories.split(" ");
                Double value=Double.valueOf(1)/Double.valueOf(app_categories.length)*(coeff);

                boolean check=false;
                for (String temp:app_categories){
                    int position=list_category.indexOf(Integer.parseInt(temp));

                    if (position>=0 && position<list_category.size()){
                        list.set(position, value);
                        check=true;
                    }
                }

                if (!check){
                    for (int i=0; i<list_category.size(); i++){
                        list.set(i, Double.valueOf(1)/(Double.valueOf(list_category.size())*(coeff)));
                    }
                }
            }
        }

        return list;
    }

    public static List<Double> analyzeGender(String gender, double coeff){
        List<Double> list=new ArrayList<Double>();
        NumberUtils.addZero(list, 1);

        if (gender==null){
            list.set(0, Double.valueOf(0.25));
        } else {
            gender=gender.replace(","," ").trim();
            if (gender.isEmpty()){
                list.set(0, Double.valueOf(0.25));
            } else {
                String[] app_gender=gender.split(" ");
                if (app_gender.length==2){
                    list.set(0, Double.valueOf(0.25));
                } else {
                    Double value=Double.valueOf(MAX_APP_GENDER - Integer.parseInt(app_gender[0]))/(Double.valueOf(MAX_APP_GENDER));
                    list.set(0, value);
                }
            }
        }

        list.set(0, list.get(0)*(coeff));
        return list;
    }


    public static void main(String args[]){
        AppReader reader=new AppReader();
        BasicMatrix matrix = reader.read();

        System.out.println (matrix.toString());
        NumberUtils.printList("application_list.txt", reader.idList);
    }
}
