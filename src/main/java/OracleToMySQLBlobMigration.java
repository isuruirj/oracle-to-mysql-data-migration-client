import java.sql.*;

public class OracleToMySQLBlobMigration {

//    public static void main(String[] args) throws SQLException, ClassNotFoundException {
//
//        // Oracle DB Connection
//        Class.forName("oracle.jdbc.OracleDriver");
//        Connection oracleConnection = DriverManager.getConnection(
//                "jdbc:oracle:thin:@localhost:1521/ORCL",
//                "SYSTEM_USERSTORE_DB",
//                "vfregistry"
//        );
//
//        // MySQL DB Connection
//        Class.forName("com.mysql.cj.jdbc.Driver");
//        Connection mysqlConnection = DriverManager.getConnection(
//                "jdbc:mysql://localhost:3306/SYSTEM_USERSTORE_DB",
//                "root",
//                "1234"
//        );
//
//        // Select data from Oracle DB
//        String selectSql = "SELECT UM_ID, UM_USER_CONFIG FROM UM_TENANT";
//        PreparedStatement selectStatement = oracleConnection.prepareStatement(selectSql);
//        ResultSet resultSet = selectStatement.executeQuery();
//
//        // Insert data into MySQL DB
//        String updateSql = "UPDATE UM_TENANT SET UM_USER_CONFIG = ? WHERE UM_ID = ?";
//        PreparedStatement insertStatement = mysqlConnection.prepareStatement(updateSql);
//
//        while(resultSet.next()) {
//            int id = resultSet.getInt("UM_ID");
//            Blob blob = resultSet.getBlob("UM_USER_CONFIG");
//
//            // Set values for MySQL insert statement
//            insertStatement.setBlob(1, blob);
//            insertStatement.setInt(2, id);
//
//            // Execute MySQL insert statement
//            insertStatement.executeUpdate();
//        }
//
//        // Close connections
//        oracleConnection.close();
//        mysqlConnection.close();
//    }
}
