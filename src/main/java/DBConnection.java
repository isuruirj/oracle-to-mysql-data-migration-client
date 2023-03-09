import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    /**
     * Static object of SQL connection.
     */
    private static Connection mysql_connection = null;
    private static Connection oracle_connection = null;

    /**
     * Create database connection if closed. Else return existing connection.
     * @param url url of database.
     * @param username username of database.
     * @param password password of database.
     * @return database connection.
     */
    public static Connection getOracleConnection(String url, String username, String password) {
        if (oracle_connection == null) {
            try {
                oracle_connection = DriverManager.getConnection(url, username, password);
            } catch (SQLException e) {
                System.out.println("Unable to connect to oracle database.");
                e.printStackTrace();
                return null;
            }
        }
        return oracle_connection;
    }

    /**
     * Create database connection if closed. Else return existing connection.
     * @param url url of database.
     * @param username username of database.
     * @param password password of database.
     * @return database connection.
     */
    public static Connection getMysqlConnection(String url, String username, String password) {
        if (mysql_connection == null) {
            try {
                mysql_connection = DriverManager.getConnection(url, username, password);
            } catch (SQLException e) {
                System.out.println("Unable to connect to mysql database.");
                e.printStackTrace();
                return null;
            }
        }
        return mysql_connection;
    }

    /**
     * Load JDBC driver.
     * @param driverLoacation location of JAR file.
     * @param jdbcConnectionClass Connection classs name.
     */
    public static void loadDBDriver(String driverLoacation, String jdbcConnectionClass) {
        File file = new File(driverLoacation);
        URL url = null;
        try {
            url = file.toURI().toURL();
        } catch (MalformedURLException e) {
            System.out.printf("Unable to open url.");
            e.printStackTrace();
        }
        URLClassLoader ucl = new URLClassLoader(new URL[] {url});
        Driver driver = null;
        try {
            driver = (Driver) Class.forName(jdbcConnectionClass, true, ucl).newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            DriverManager.registerDriver(new DriverShim(driver));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
