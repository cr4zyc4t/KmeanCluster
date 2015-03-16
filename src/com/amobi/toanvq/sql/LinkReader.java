package com.amobi.toanvq.sql;


import com.amobi.toanvq.jsp.AmobiClusterK;
import com.amobi.toanvq.utils.MatrixUtilities;
import com.amobi.toanvq.utils.NumberUtils;
import org.ojalgo.matrix.BasicMatrix;
import org.ojalgo.matrix.PrimitiveMatrix;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class LinkReader {
    public static final String CLICKS="clicks";
    public static final String VIEWS="views";
    public static final String WIDGET_CODE="widget_code";
    public static final String LINK_ID="link_id";

    private List<Integer> listAdvIndex;
    private List<String> listAppIndex;
    private HashMap<Integer, Integer> linkIdConverter;

    private BasicMatrix.Factory<PrimitiveMatrix> factory= PrimitiveMatrix.FACTORY;

    public LinkReader(List<Integer> listAdvIndex, List<String> listAppIndex, HashMap<Integer, Integer> linkIdConverter) {
        this.listAdvIndex = listAdvIndex;
        this.listAppIndex = listAppIndex;
        this.linkIdConverter = linkIdConverter;
    }

    public BasicMatrix read(){
        try{
            Class.forName(Config.DB_DRIVER);
            Connection connection= DriverManager.getConnection(Config.DB_URL, Config.DB_USER, Config.DB_PASSWORD);

            String sql="SELECT * FROM widget_publisher_code";

            ResultSet resultSet=connection.createStatement().executeQuery(sql);

            int numRows=listAdvIndex.size();
            int numColumns=listAppIndex.size();

            System.out.println (numRows +" "+numColumns);
            double[][] array=new double[numRows][numColumns];
            for (int i=0; i<numRows; i++){
                Arrays.fill(array[i], Double.valueOf(0));
            }

            while (resultSet.next()){
                int clicks=resultSet.getInt(CLICKS);
                int views=resultSet.getInt(VIEWS);

                // choose value=clicks+views;
                double value=Double.valueOf(clicks)/Double.valueOf(views);//+Double.valueOf(views);

                String code=resultSet.getString(WIDGET_CODE);
                int linkID=resultSet.getInt(LINK_ID);

                int appIndex=listAppIndex.indexOf(code);
                int advIndex=listAdvIndex.indexOf(linkIdConverter.get(linkID));

                if (appIndex>=0 & advIndex>=0){
                    array[advIndex][appIndex]=array[advIndex][appIndex]+(Double.valueOf(value));
                }
            }

            BasicMatrix matrix=factory.rows(array);

            connection.close();
            return matrix;

        } catch (ClassNotFoundException e){
            e.printStackTrace();
            return null;
        } catch (SQLException e){
            e.printStackTrace();
            return null;
        }
    }


    public static void main(String args[]){

        AdReader adReader = new AdReader();
        AppReader appReader = new AppReader();

        System.out.println("Reading Advertisement BasicMatrix.....");

        BasicMatrix adMatrix = adReader.read();
        adMatrix = MatrixUtilities.normalizeMatrix(adMatrix);
        //MatrixUtilities.toFile("AdMatrix.csv", adMatrix);


        System.out.println("Reading Application BasicMatrix.....");
        BasicMatrix appMatrix = appReader.read();
        appMatrix = MatrixUtilities.normalizeMatrix(appMatrix);
        //MatrixUtilities.toFile("AppMatrix.csv", appMatrix);

        LinkReader linkReader = new LinkReader(adReader.idList, appReader.idList, new LinkConverter().getLinkIDAdvIDMap());

        System.out.println("Reading Adv-App Relation BasicMatrix...");
        BasicMatrix adAppMatrix = linkReader.read();
        BasicMatrix appAdMatrix = adAppMatrix.transpose();

        adAppMatrix = MatrixUtilities.normalizeMatrix(adAppMatrix);
        appAdMatrix = MatrixUtilities.normalizeMatrix(appAdMatrix);

        String s="";

        int count=0;
        for (int i=0; i<adAppMatrix.countRows(); i++){
            double max = 0;
            for (int j=0; j<adAppMatrix.countColumns(); j++){
                if (adAppMatrix.get(i, j).doubleValue() > max){
                    max = adAppMatrix.get(i, j).doubleValue();
                }
            }
            if (max > 0){
                count ++;
                System.out.println (max);
            }
        }

        System.out.println ("Ti le quang cao co click > 0: " + count*100/adAppMatrix.countRows()+" %");
    }
}
