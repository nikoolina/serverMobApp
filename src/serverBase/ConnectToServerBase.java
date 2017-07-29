package serverBase;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.sql.Connection;
import java.sql.DriverManager;

/**
 *
 * @author tin
 */
public class ConnectToServerBase {
   
    /**
     * setting up a connection
     *
     * 
     * @throws Exception
     */
    public static Connection getConnection() throws Exception {

        try {

            String driver = "com.mysql.jdbc.Driver";
            String url ="jdbc:mysql://localhost:3306/Server?characterEncoding=UTF-8&useSSL=false";
			String username = "root";
            String password = "cres";
            Class.forName(driver);

            Connection conn = DriverManager.getConnection(url, username, password);
            System.out.println("Connected to DataBase Server!");
            return conn;
        } catch (Exception e) {
            System.out.println("nisam usao u bazu");
            System.out.println(e);
        }

        return null;
        
        
    }
}