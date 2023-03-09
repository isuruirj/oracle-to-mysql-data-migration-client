import com.oracle.javafx.jmx.json.JSONException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wso2.carbon.identity.application.common.model.CertificateInfo;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationManagementUtil;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.config.RealmConfigXMLProcessor;
import org.wso2.carbon.registry.core.utils.RegistryUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class BlobMigration {

    public static void main(String[] args) throws InterruptedException {

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
        System.out.println("====== Tenant Data Migration Completed ======");
        migrateRegContent(oracleUrl, oracleRegDBUsername, oracleRegDBPassword, mysqlRegDBUrl, mysqlRegDBUsername, mysqlRegDBPassword);
        System.out.println("====== Reg Content Data Migration Completed ======");
        migrateAdaptiveScript(oracleUrl, oracleIdentityDBUsername, oracleIdentityDBPassword, mysqlIdentityDBUrl, mysqlIdentityDBUsername, mysqlIdentityDBPassword);
        System.out.println("====== Adaptive Script Data Migration Completed ======");
        migrateIdpCertificate(oracleUrl, oracleIdentityDBUsername, oracleIdentityDBPassword, mysqlIdentityDBUrl, mysqlIdentityDBUsername, mysqlIdentityDBPassword);
        System.out.println("====== IDP Certificate Migration Completed ======");
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
                InputStream is = new ByteArrayInputStream(realmConfig.getBytes());
                pstmt.setBinaryStream(1, is, is.available());
                pstmt.setInt(2, id);
                pstmt.executeUpdate();
            }
            oracleConn.close();
            mysqlConn.close();
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

    public static void migrateRegContent(String oracleUrl, String oracleUsername, String oraclePassword, String mysqlUrl, String mysqlUsername, String mysqlPassword) {
        try {
            Connection oracleConn = DBConnection.getOracleConnection(oracleUrl, oracleUsername, oraclePassword);
            Connection mysqlConn = DBConnection.getMysqlConnection(mysqlUrl, mysqlUsername, mysqlPassword);

            String selectQuery = "SELECT REG_CONTENT_ID, REG_TENANT_ID, REG_CONTENT_DATA FROM REG_CONTENT";
            Statement stmt = oracleConn.createStatement();
            ResultSet rs = stmt.executeQuery(selectQuery);

            String updateQuery = "UPDATE REG_CONTENT SET REG_CONTENT_DATA = ? WHERE REG_CONTENT_ID = ? AND REG_TENANT_ID = ?";
            PreparedStatement pstmt = mysqlConn.prepareStatement(updateQuery);

            while (rs.next()) {
                int regContentId = rs.getInt("REG_CONTENT_ID");
                int regTenantId = rs.getInt("REG_TENANT_ID");
                InputStream inputStream = rs.getBinaryStream("REG_CONTENT_DATA");
                inputStream = RegistryUtils.getMemoryStream(inputStream);
                pstmt.setBinaryStream(1, inputStream, inputStream.available());
                pstmt.setInt(2, regContentId);
                pstmt.setInt(3, regTenantId);
                pstmt.executeUpdate();
            }
            oracleConn.close();
            mysqlConn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void migrateAdaptiveScript(String oracleUrl, String oracleUsername, String oraclePassword, String mysqlUrl, String mysqlUsername, String mysqlPassword) {

        try {
            Connection oracleConn = DBConnection.getOracleConnection(oracleUrl, oracleUsername, oraclePassword);
            Connection mysqlConn = DBConnection.getMysqlConnection(mysqlUrl, mysqlUsername, mysqlPassword);

            String selectQuery = "SELECT TENANT_ID, APP_ID, CONTENT FROM SP_AUTH_SCRIPT";
            Statement stmt = oracleConn.createStatement();
            ResultSet rs = stmt.executeQuery(selectQuery);

            String updateQuery = "UPDATE SP_AUTH_SCRIPT SET CONTENT = ? WHERE TENANT_ID = ? AND APP_ID = ?";
            PreparedStatement pstmt = mysqlConn.prepareStatement(updateQuery);

            while (rs.next()) {
                int tenantId = rs.getInt("TENANT_ID");
                int appId = rs.getInt("APP_ID");
                InputStream inputStream = rs.getBinaryStream("CONTENT");
                String targetString = "";
                if (inputStream != null) {
                    targetString = IOUtils.toString(inputStream);
                }
                InputStream is = new ByteArrayInputStream(targetString.getBytes());
                pstmt.setBinaryStream(1, is, is.available());
                pstmt.setInt(2, tenantId);
                pstmt.setInt(3, appId);
                pstmt.executeUpdate();
            }
            oracleConn.close();
            mysqlConn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void migrateIdpCertificate(String oracleUrl, String oracleUsername, String oraclePassword, String mysqlUrl, String mysqlUsername, String mysqlPassword) {
        try {
            Connection oracleConn = DBConnection.getOracleConnection(oracleUrl, oracleUsername, oraclePassword);
            Connection mysqlConn = DBConnection.getMysqlConnection(mysqlUrl, mysqlUsername, mysqlPassword);

            String selectQuery = "SELECT TENANT_ID, NAME, CERTIFICATE FROM IDP";
            Statement stmt = oracleConn.createStatement();
            ResultSet rs = stmt.executeQuery(selectQuery);

            String updateQuery = "UPDATE IDP SET CERTIFICATE = ? WHERE TENANT_ID = ? AND NAME = ?";
            PreparedStatement pstmt = mysqlConn.prepareStatement(updateQuery);

            while (rs.next()) {
                int tenantId = rs.getInt("TENANT_ID");
                String name = rs.getString("NAME");
                InputStream inputStream = rs.getBinaryStream("CERTIFICATE");
                String certificate = getBlobValue(inputStream);
                JSONArray certificateInfoJsonArray = new JSONArray(getCertificateInfoArray(certificate));
                InputStream is = new ByteArrayInputStream(certificateInfoJsonArray.toString().getBytes());
                pstmt.setBinaryStream(1, is, is.available());
                pstmt.setInt(2, tenantId);
                pstmt.setString(3, name);
                pstmt.executeUpdate();
            }
            oracleConn.close();
            mysqlConn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getBlobValue(InputStream is) {

        if (is != null) {
            BufferedReader br = null;
            StringBuilder sb = new StringBuilder();
            String line;
            try {
                br = new BufferedReader(new InputStreamReader(is));
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            } catch (IOException e) {

            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {

                    }
                }
            }
            return sb.toString();
        }
        return null;
    }

    public static final String PEM_BEGIN_CERTFICATE = "-----BEGIN CERTIFICATE-----";
    public static final String PEM_END_CERTIFICATE = "-----END CERTIFICATE-----";

    private static CertificateInfo[] getCertificateInfoArray(String certificateValue) {
        try {
            if (StringUtils.isNotBlank(certificateValue) && !certificateValue.equals("[]")) {
                certificateValue = certificateValue.trim();
                try {
                    return handleJsonFormatCertificate(certificateValue);
                } catch (JSONException e) {
                    // Handle plain text certificate for file based configuration.
                    if (certificateValue.startsWith(PEM_BEGIN_CERTFICATE)) {
                        return handlePlainTextCertificate(certificateValue);
                    } else {
                        // Handle encoded certificate values. While uploading through UI and file based configuration
                        // without begin and end statement.
                        return handleEncodedCertificate(certificateValue);
                    }
                }
            }
            return new CertificateInfo[0];
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error while generating thumbPrint. Unsupported hash algorithm. ");
            e.printStackTrace();
            return new CertificateInfo[0];
        }
    }

    private static CertificateInfo[] handleJsonFormatCertificate(String certificateValue) throws NoSuchAlgorithmException {

        JSONArray jsonCertificateInfoArray = new JSONArray(certificateValue);
        int lengthOfJsonArray = jsonCertificateInfoArray.length();

        List<CertificateInfo> certificateInfos = new ArrayList<>();
        for (int i = 0; i < lengthOfJsonArray; i++) {
            JSONObject jsonCertificateInfoObject = (JSONObject) jsonCertificateInfoArray.get(i);
            String thumbPrint = jsonCertificateInfoObject.getString("thumbPrint");

            CertificateInfo certificateInfo = new CertificateInfo();
            certificateInfo.setThumbPrint(thumbPrint);
            certificateInfo.setCertValue(jsonCertificateInfoObject.getString("certValue"));
            certificateInfos.add(certificateInfo);
        }
        return certificateInfos.toArray(new CertificateInfo[lengthOfJsonArray]);
    }

    private static CertificateInfo[] handlePlainTextCertificate(String certificateValue) throws NoSuchAlgorithmException {

        return createEncodedCertificateInfo(certificateValue, false);
    }

    private static CertificateInfo[] createEncodedCertificateInfo(String decodedCertificate, boolean isEncoded) throws
            NoSuchAlgorithmException {

        int numberOfCertificates = StringUtils.countMatches(decodedCertificate, PEM_BEGIN_CERTFICATE);
        List<CertificateInfo> certificateInfoArrayList = new ArrayList<>();
        for (int ordinal = 1; ordinal <= numberOfCertificates; ordinal++) {
            String certificateVal;
            if (isEncoded) {
                certificateVal = Base64.getEncoder().encodeToString(IdentityApplicationManagementUtil.extractCertificate
                        (decodedCertificate, ordinal).getBytes(StandardCharsets.UTF_8));
            } else {
                certificateVal = IdentityApplicationManagementUtil.extractCertificate(decodedCertificate, ordinal).
                        replace(PEM_BEGIN_CERTFICATE, "").replace(
                                PEM_END_CERTIFICATE, "");
            }
            CertificateInfo certificateInfo = new CertificateInfo();
            String thumbPrint = IdentityApplicationManagementUtil.generateThumbPrint(certificateVal);

            certificateInfo.setThumbPrint(thumbPrint);
            certificateInfo.setCertValue(certificateVal);
            certificateInfoArrayList.add(certificateInfo);
        }
        return certificateInfoArrayList.toArray(new CertificateInfo[numberOfCertificates]);
    }

    private static CertificateInfo[] handleEncodedCertificate(String certificateValue) throws NoSuchAlgorithmException {

        String decodedCertificate;
        try {
            decodedCertificate = new String(Base64.getDecoder().decode(certificateValue));
        } catch (IllegalArgumentException ex) {
            // TODO Need to handle the exception handling in proper way.
            return createCertificateInfoForNoBeginCertificate(certificateValue);
        }
        if (StringUtils.isNotBlank(decodedCertificate) && !decodedCertificate.startsWith(PEM_BEGIN_CERTFICATE)) {
            // Handle certificates which are one time encoded but doesn't have BEGIN and END statement
            return createCertificateInfoForNoBeginCertificate(certificateValue);
        } else {
            return createEncodedCertificateInfo(decodedCertificate, true);
        }
    }

    private static CertificateInfo[] createCertificateInfoForNoBeginCertificate(String certificateValue) throws NoSuchAlgorithmException {

        String encodedCertVal = Base64.getEncoder().encodeToString(certificateValue.getBytes());
        String thumbPrint = IdentityApplicationManagementUtil.generateThumbPrint(encodedCertVal);
        List<CertificateInfo> certificateInfoList = new ArrayList<>();
        CertificateInfo certificateInfo = new CertificateInfo();
        certificateInfo.setThumbPrint(thumbPrint);
        certificateInfo.setCertValue(certificateValue);
        certificateInfoList.add(certificateInfo);
        return certificateInfoList.toArray(new CertificateInfo[1]);
    }
}
