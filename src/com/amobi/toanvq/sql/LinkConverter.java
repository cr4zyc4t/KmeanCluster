package com.amobi.toanvq.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class LinkConverter {
    public HashMap<Integer, Integer> getLinkIDAdvIDMap(){
        // Map <LinkID, AdvID>
        try{
            Class.forName(Config.DB_DRIVER);
            Connection connection= DriverManager.getConnection(Config.DB_URL, Config.DB_USER, Config.DB_PASSWORD);

            String sql="SELECT id, adv_id FROM link";
            ResultSet resultSet=connection.createStatement().executeQuery(sql);

            HashMap<Integer, Integer> map=new HashMap<Integer, Integer>();
            while (resultSet.next()){
                map.put(resultSet.getInt("id"), resultSet.getInt("adv_id"));
            }

            connection.close();
            return map;

        } catch (ClassNotFoundException e){
            e.printStackTrace();
            return null;
        } catch (SQLException e){
            e.printStackTrace();
            return null;
        }
    }
    public static void main(String args[]){
        System.out.print(new LinkConverter().getLinkIDAdvIDMap());
    }
}
