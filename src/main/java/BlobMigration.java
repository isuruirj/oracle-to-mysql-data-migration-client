import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.config.RealmConfigXMLProcessor;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class BlobMigration {

    public static void main(String[] args) throws IOException, UserStoreException {

        ReadConfigFile configs = new ReadConfigFile();
        String oracleUrl = configs.getProperty("CONNECTION.ORACLE.URL");
        String oracleDriverClass = configs.getProperty("CONNECTION.ORACLE.DRIVERCLASS");
        String oracleDriverLocation = configs.getProperty("CONNECTION.ORACLE.JDBCDRIVER");
        String oracleIdentityDBUsername = configs.getProperty("CONNECTION.ORACLE.IDENTITYDB.USERNAME");
        String oracleIdentityDBPassword = configs.getProperty("CONNECTION.ORACLE.IDENTITYDB.PASSWORD");
        String oracleRegDBUsername = configs.getProperty("CONNECTION.ORACLE.REGDB.USERNAME");
        String oracleRegDBPassword = configs.getProperty("CONNECTION.ORACLE.REGDB.PASSWORD");
        String oracleUserstoreDBUsername = configs.getProperty("CONNECTION.ORACLE.USERSTOREDB.USERNAME");
        String oracleUserstoreDBPassword = configs.getProperty("CONNECTION.ORACLE.USERSTOREDB.PASSWORD");

        String mysqlDriverClass = configs.getProperty("CONNECTION.MYSQL.DRIVERCLASS");
        String mysqlDriverLocation = configs.getProperty("CONNECTION.MYSQL.JDBCDRIVER");
        String mysqlIdentityDBUrl = configs.getProperty("CONNECTION.MYSQL.IDENTITYDB.URL");
        String mysqlIdentityDBUsername = configs.getProperty("CONNECTION.MYSQL.IDENTITYDB.USERNAME");
        String mysqlIdentityDBPassword = configs.getProperty("CONNECTION.MYSQL.IDENTITYDB.PASSWORD");
        String mysqlRegDBUrl = configs.getProperty("CONNECTION.MYSQL.REGDB.URL");
        String mysqlRegDBUsername = configs.getProperty("CONNECTION.MYSQL.REGDB.USERNAME");
        String mysqlRegDBPassword = configs.getProperty("CONNECTION.MYSQL.REGDB.PASSWORD");
        String mysqlUserstoreDBUrl = configs.getProperty("CONNECTION.MYSQL.USERSTOREDB.URL");
        String mysqlUserstoreDBUsername = configs.getProperty("CONNECTION.MYSQL.USERSTOREDB.USERNAME");
        String mysqlUserstoreDBPassword = configs.getProperty("CONNECTION.MYSQL.USERSTOREDB.PASSWORD");

        DBConnection.loadDBDriver(oracleDriverLocation, oracleDriverClass);
        DBConnection.loadDBDriver(mysqlDriverLocation, mysqlDriverClass);

        migrateUMTenant(oracleUrl, oracleUserstoreDBUsername, oracleUserstoreDBPassword, mysqlUserstoreDBUrl, mysqlUserstoreDBUsername, mysqlUserstoreDBPassword);
    }

    public static void migrateUMTenant(String oracleUrl, String oracleUsername, String oraclePassword, String mysqlUrl, String mysqlUsername, String mysqlPassword){

        try {
            Connection oracleConn = DBConnection.getOracleConnection(oracleUrl, oracleUsername, oraclePassword);
            Connection mysqlConn = DBConnection.getMysqlConnection(mysqlUrl, mysqlUsername, mysqlPassword);

            String selectQuery = "SELECT UM_ID, UM_USER_CONFIG FROM UM_TENANT";
            Statement stmt = oracleConn.createStatement();
            ResultSet rs = stmt.executeQuery(selectQuery);
            String realmConfig;

            String updateQuery = "UPDATE UM_TENANT SET UM_USER_CONFIG = ? WHERE UM_ID = ?";
            PreparedStatement pstmt = mysqlConn.prepareStatement(updateQuery);

            while (rs.next()) {
                int id = rs.getInt("UM_ID");
                InputStream inputStream = rs.getBinaryStream("UM_USER_CONFIG");
                RealmConfigXMLProcessor processor = new RealmConfigXMLProcessor();
                RealmConfiguration realmConfiguration = processor.buildTenantRealmConfiguration(inputStream);
                realmConfig = RealmConfigXMLProcessor.serialize(realmConfiguration).toString();
                InputStream is = null;
                is = new ByteArrayInputStream(realmConfig.getBytes());
                pstmt.setBinaryStream(1, is, is.available());
                pstmt.setInt(2, id);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (UserStoreException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
