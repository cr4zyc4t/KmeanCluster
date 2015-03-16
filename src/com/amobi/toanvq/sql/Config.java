package com.amobi.toanvq.sql;

public class Config {
    // Config of JDBC
    public static final String DB_DRIVER = "com.mysql.jdbc.Driver";
    public static final String DB_URL = "jdbc:mysql://localhost/amobi.vn";
    public static final String DB_USER = "root";
    public static final String DB_PASSWORD = "";
    public static final String ADV_CATEGORIES="adv_categories";

    public static String CONFIGURATION="configuration.xml";

    public static void setCONFIGURATION(String CONFIGURATION) {
        Config.CONFIGURATION = CONFIGURATION;
    }

}
