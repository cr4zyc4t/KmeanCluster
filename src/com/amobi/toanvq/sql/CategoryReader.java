package com.amobi.toanvq.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoryReader {

    public static List<Integer> getListCategory(){
        try {

            Class.forName(Config.DB_DRIVER);
            Connection connection = DriverManager.getConnection(Config.DB_URL, Config.DB_USER, Config.DB_PASSWORD);

            List<Integer> map=new ArrayList<Integer>();

            String sql="SELECT * FROM "+ Config.ADV_CATEGORIES;
            ResultSet resultSet=connection.createStatement().executeQuery(sql);

            int i=0;
            while (resultSet.next()){
                map.add(resultSet.getInt("id"));
            }
            connection.close();
            return map;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (SQLException e){
            e.printStackTrace();
            return null;
        }
    }
}
