/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * The author of this program can be reached at thomas@infolab.northwestern.edu
 **/
package soc.server.database;

import soc.game.SOCGame;
import soc.game.SOCPlayer;

import soc.util.SOCPlayerInfo;
import soc.util.SOCRobotParameters;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import java.util.Calendar;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;


/**
 * This class contains methods for connecting to a database
 * and for manipulating the data stored there.
 *
 * Based on jdbc code found at www.javaworld.com
 *
 * @author Robert S. Thomas
 */
/**
 * This code assumes that you're using mySQL as your database. The schema for
 * JSettlers tables can be found in the distribution
 * <code>$JSETTLERS/bin/sql/jsettlers-tables.sql</code>.
 */
public class SOCDBHelper
{
    /** Property to specify the SQL database server. */
    public static final String JSETTLERS_DB_ENABLED = "jsettlers.db.enabled";

    /** Property to specify the SQL database username. */
    public static final String JSETTLERS_DB_USER = "jsettlers.db.user";

    /** Property to specify the SQL database password. */
    public static final String JSETTLERS_DB_PASS = "jsettlers.db.pass";

    /** Property to specify the SQL database type. */
    public static final String JSETTLERS_DB_DRIVER = "jsettlers.db.driver";

    /** Property to specify the SQL database server. */
    public static final String JSETTLERS_DB_URL = "jsettlers.db.url";

    private static Connection connection = null;

    /**
     * This flag indicates that the connection should be valid, yet the last
     * operation failed. Methods will attempt to reconnect prior to their
     * operation if this is set.
     */
    private static boolean errorCondition = false;

    /** Cached username used when reconnecting on error */
    private static String userName;

    /** Cached password used when reconnecting on error */
    private static String password;
    
    /** Cached url used when reconnecting on error */
    private static String url;
    
    private static String CREATE_ACCOUNT_COMMAND =  "INSERT INTO users VALUES (?,?,?,?,?,?,?,?,?);";
    
    private static String HOST_QUERY =              "SELECT nickname FROM users WHERE ( users.host = ? );";
    
    private static String HUMAN_STATS_QUERY =       "SELECT nickname, wins, losses, totalpoints, totalpoints/(wins+losses) AS avg, 100 * (wins/(wins+losses)) AS pct FROM users WHERE (wins+losses) > 0 ORDER BY pct desc, avg desc, totalpoints desc;";
    
    private static String LASTLOGIN_UPDATE =        "UPDATE users SET lastlogin = ? WHERE nickname = ? ;";
    
    private static String RECORD_LOGIN_COMMAND =    "INSERT INTO logins VALUES (?,?,?);";
    
    private static String RESET_HUMAN_STATS =       "UPDATE users SET wins = 0, losses = 0, totalpoints = 0 WHERE nickname = ?;";
    
    private static String ROBOT_PARAMS_QUERY =      "SELECT * FROM robotparams WHERE robotname = ?;";
    
    private static String ROBOT_STATS_QUERY =       "SELECT robotname, wins, losses, totalpoints, totalpoints/(wins+losses) AS avg, (100*(wins/(wins+losses))) AS pct FROM robotparams WHERE (wins+losses) > 0 ORDER BY pct desc, avg desc, totalpoints desc;";
    
    private static String SAVE_GAME_COMMAND =       "INSERT INTO games VALUES (?,?,?,?,?,?,?,?,?,?);";
    
    private static String UPDATE_ROBOT_STATS =      "UPDATE robotparams SET wins = wins + ?, losses = losses + ?, totalpoints = totalpoints + ? WHERE robotname = ?;";
    
    private static String UPDATE_USER_STATS = "UPDATE users SET wins = wins + ?, losses = losses + ?, totalpoints = totalpoints + ? WHERE nickname = ?;";
    
    private static String USER_FACE_QUERY =         "SELECT face FROM users WHERE users.nickname = ?;";
    
    private static String USER_FACE_UPDATE =        "UPDATE users SET face = ? WHERE nickname = ?;";
    
    private static String USER_PASSWORD_QUERY =     "SELECT password FROM users WHERE ( users.nickname = ? );";
    
    private static PreparedStatement createAccountCommand = null;    
    private static PreparedStatement hostQuery = null;
    private static PreparedStatement lastloginUpdate = null;
    private static PreparedStatement recordLoginCommand = null;
    private static PreparedStatement resetHumanStats = null;
    private static PreparedStatement robotParamsQuery = null;
    private static PreparedStatement saveGameCommand = null;
    private static PreparedStatement updateRobotStats = null;
    private static PreparedStatement updateUserStats = null;
    private static PreparedStatement userFaceQuery = null;
    private static PreparedStatement userFaceUpdate = null;
    private static PreparedStatement userPasswordQuery = null;    

    /**
     * This makes a connection to the database
     * and initializes the prepared statements.
     * If props = null, we behave as though db.enabled=false;
     *
     * @param props  Properties containing minimally username, password,
     * connection url and database driver class, or null
     * @return true if the database was initialized
     * @throws SQLException if an SQL command fails, or the db couldn't be
     * initialied
     */
    public static boolean initialize(Properties props) throws SQLException
    {
        if (isConnected())
            throw new IllegalStateException("Database already initialized");

        boolean enabled = false;
        if (props != null)
        {
            String p = props.getProperty(SOCDBHelper.JSETTLERS_DB_ENABLED);
            enabled = Boolean.valueOf(p).booleanValue();
        }
        if (! enabled)
            return false;
        
        // extract info from properties
        userName = props.getProperty(JSETTLERS_DB_USER);
        password = props.getProperty(JSETTLERS_DB_PASS);
        url = props.getProperty(JSETTLERS_DB_URL);
        String driver = props.getProperty(JSETTLERS_DB_DRIVER);

        if (driver == null)
            throw new SQLException("SQL driver not specified");
        if (url == null)
            throw new SQLException("SQL url not specified");

        try
        {
            Class.forName(driver).newInstance();
            connect();
        }
        catch (ClassNotFoundException x)
        {
            SQLException sx = new SQLException("SQL driver not found: " + driver);
            sx.initCause(x);
            throw sx;
        }
        catch (Exception x) // everything else
        {
            SQLException sx = new SQLException("Unable to initialize user database: " + url);
            sx.initCause(x);
            throw sx;
        }
        
        return isConnected();
    }

    /**
     * Returns true if connection has been made. Does not attempt to reconnect
     * if an error has occured.
     *
     * @return true if the connection has been made
     */
    public static boolean isConnected() throws SQLException
    {
        return connection != null;
    }

    /**
     * Checks if connection is supposed to be present and attempts to reconnect
     * if there was previously an error.  Reconnecting closes the current
     * conection, opens a new one, and re-initializes the prepared statements.
     *
     * @return true if the connection is established upon return
     */
    private static boolean checkConnection() throws SQLException
    {
        if (connection != null)
        {
            return (! errorCondition) || connect();
        }

        return false;
    }

    /**
     * initialize and checkConnection use this to get ready.
     */
    private static boolean connect()
        throws SQLException
    {
        connection = DriverManager.getConnection(url, userName, password);

        errorCondition = false;
        
        // prepare PreparedStatements for queries
        createAccountCommand = connection.prepareStatement(CREATE_ACCOUNT_COMMAND);
        hostQuery = connection.prepareStatement(HOST_QUERY);
        lastloginUpdate = connection.prepareStatement(LASTLOGIN_UPDATE);
        recordLoginCommand = connection.prepareStatement(RECORD_LOGIN_COMMAND);
        resetHumanStats = connection.prepareStatement(RESET_HUMAN_STATS);
        robotParamsQuery = connection.prepareStatement(ROBOT_PARAMS_QUERY);
        saveGameCommand = connection.prepareStatement(SAVE_GAME_COMMAND);
        userFaceQuery = connection.prepareStatement(USER_FACE_QUERY);
        userFaceUpdate = connection.prepareStatement(USER_FACE_UPDATE);
        userPasswordQuery = connection.prepareStatement(USER_PASSWORD_QUERY);
        updateRobotStats = connection.prepareStatement(UPDATE_ROBOT_STATS);
        updateUserStats = connection.prepareStatement(UPDATE_USER_STATS);
        
        return true;
    }
    
    /**
     * DOCUMENT ME!
     *
     * @param sUserName DOCUMENT ME!
     *
     * @return null if user account doesn't exist
     *
     * @throws SQLException DOCUMENT ME!
     */
    public static String getUserPassword(String sUserName) throws SQLException
    {
        String password = null;

        // ensure that the JDBC connection is still valid
        if (checkConnection())
        {
            try
            {
                // fill in the data values to the Prepared statement
                userPasswordQuery.setString(1, sUserName);

                // execute the Query
                ResultSet resultSet = userPasswordQuery.executeQuery();

                // if no results, user is not authenticated
                if (resultSet.next())
                {
                    password = resultSet.getString(1);
                }

                resultSet.close();
            }
            catch (SQLException sqlE)
            {
                handleSQLException(sqlE);
            }
        }

        return password;
    }

    /**
     * Authenticate a user with the specified password. If authentication is
     * successful, the users lastlogin is updated and Auth.PASS is returned. If
     * the username is unknown, or the database is inaccessable,
     * Auth.UNKNOWN, and finally Auth.FAIL for an incorrect password.
     *
     * @param userName name of player
     * @param password password of player
     *
     * @return <code>Auth.PASS</code>, <code>Auth.FAIL</code>, or
     * <code>Auth.UNKNOWN</code>
     * @throws SQLException on db error
     * @throws NullPointerException if either parameter is null
     */
    public static Auth authenticate(String userName, String password, String host) throws SQLException
    {
        Auth result = Auth.UNKNOWN;
        
        // ensure that the JDBC connection is still valid
        if (checkConnection())
        {
            try
            {
                // fill in the data values to the Prepared statement
                userPasswordQuery.setString(1, userName);

                // execute the Query
                ResultSet resultSet = userPasswordQuery.executeQuery();

                // if no results, user is not authenticated
                if (resultSet.next())
                {
                    String dbPass = resultSet.getString(1);
                    if (password != null && password.equals(dbPass))
                    {
                        recordLogin(userName, host, System.currentTimeMillis());
                        result = Auth.PASS;
                    }
                    else
                    {
                        result = Auth.FAIL;
                    }
                }

                resultSet.close();
            }
            catch (SQLException sqlE)
            {
                handleSQLException(sqlE);
            }
        }

        return result;
    }
    

    /**
     * returns default face for player
     *
     * @param sUserName username of player
     *
     * @return 1 if user account doesn't exist
     *
     * @throws SQLException if database is horked
     */
    public static int getUserFace(String sUserName) throws SQLException
    {
        int face = 1;

        // ensure that the JDBC connection is still valid
        if (checkConnection())
        {
            try
            {
                // fill in the data values to the Prepared statement
                userFaceQuery.setString(1, sUserName);

                // execute the Query
                ResultSet resultSet = userFaceQuery.executeQuery();

                // if no results, user is not authenticated
                if (resultSet.next())
                {
                    face = resultSet.getInt(1);
                }

                resultSet.close();
            }
            catch (SQLException sqlE)
            {
                handleSQLException(sqlE);
            }
        }

        return face;
    }

    /**
     * DOCUMENT ME!
     *
     * @param host DOCUMENT ME!
     *
     * @return  null if user is not authenticated
     *
     * @throws SQLException DOCUMENT ME!
     */
    public static String getUserFromHost(String host) throws SQLException
    {
        String nickname = null;

        // ensure that the JDBC connection is still valid
        if (checkConnection())
        {
            try
            {
                // fill in the data values to the Prepared statement
                hostQuery.setString(1, host);

                // execute the Query
                ResultSet resultSet = hostQuery.executeQuery();

                // if no results, user is not authenticated
                if (resultSet.next())
                {
                    nickname = resultSet.getString(1);
                }

                resultSet.close();
            }
            catch (SQLException sqlE)
            {
                handleSQLException(sqlE);
            }
        }

        return nickname;
    }

    /**
     * DOCUMENT ME!
     *
     * @param userName DOCUMENT ME!
     * @param host DOCUMENT ME!
     * @param password DOCUMENT ME!
     * @param email DOCUMENT ME!
     * @param time DOCUMENT ME!
     *
     * @return true if the account was created
     *
     * @throws SQLException DOCUMENT ME!
     */
    public static boolean createAccount(String userName, String host, String password, String email, long time) throws SQLException
    {
        // ensure that the JDBC connection is still valid
        if (checkConnection())
        {
            try
            {
                java.sql.Date sqlDate = new java.sql.Date(time);
                Calendar cal = Calendar.getInstance();

                // fill in the data values to the Prepared statement
                createAccountCommand.setString(1, userName);
                createAccountCommand.setString(2, host);
                createAccountCommand.setString(3, password);
                createAccountCommand.setString(4, email);
                createAccountCommand.setDate(5, sqlDate, cal);
                createAccountCommand.setInt(6, 0); // wins
                createAccountCommand.setInt(7, 0); // losses
                createAccountCommand.setInt(8, 1); // face
                createAccountCommand.setInt(9, 0); // totalpoints

                // execute the Command
                createAccountCommand.executeUpdate();

                return true;
            }
            catch (SQLException sqlE)
            {
                handleSQLException(sqlE);
            }
        }

        return false;
    }

    /**
     * DOCUMENT ME!
     *
     * @param userName DOCUMENT ME!
     * @param host DOCUMENT ME!
     * @param time DOCUMENT ME!
     *
     * @return true if the login was recorded
     *
     * @throws SQLException DOCUMENT ME!
     */
    public static boolean recordLogin(String userName, String host, long time) throws SQLException
    {
        // ensure that the JDBC connection is still valid
        if (checkConnection())
        {
            try
            {
                java.sql.Date sqlDate = new java.sql.Date(time);
                Calendar cal = Calendar.getInstance();

                // fill in the data values to the Prepared statement
                recordLoginCommand.setString(1, userName);
                recordLoginCommand.setString(2, host);
                recordLoginCommand.setDate(3, sqlDate, cal);

                // execute the Command
                recordLoginCommand.executeUpdate();

                // update the last login time
                updateLastlogin(userName, time);
                return true;
            }
            catch (SQLException sqlE)
            {
                handleSQLException(sqlE);
            }
        }

        return false;
    }

    /**
     * DOCUMENT ME!
     *
     * @param userName DOCUMENT ME!
     * @param time DOCUMENT ME!
     *
     * @return true if the save succeeded
     *
     * @throws SQLException DOCUMENT ME!
     */
    public static boolean updateLastlogin(String userName, long time) throws SQLException
    {
        // ensure that the JDBC connection is still valid
        if (checkConnection())
        {
            try
            {
                java.sql.Date sqlDate = new java.sql.Date(time);
                Calendar cal = Calendar.getInstance();

                // fill in the data values to the Prepared statement
                lastloginUpdate.setDate(1, sqlDate, cal);
                lastloginUpdate.setString(2, userName);

                // execute the Command
                lastloginUpdate.executeUpdate();

                return true;
            }
            catch (SQLException sqlE)
            {
                handleSQLException(sqlE);
            }
        }

        return false;
    }

    /**
     * Saves faceId to the database 
     *
     * @param ga game to be saved
     *
     * @return true if the save succeeded
     *
     * @throws SQLException if the database isn't available
     */
    public static boolean saveFaces(SOCGame ga) throws SQLException
    {
        // Insure that the JDBC connection is still valid
        if (checkConnection())
        {
            try
            {
                // Record face for humans
                for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                {
                    SOCPlayer pl = ga.getPlayer(i);
                    
                    // If the player is human
                    if (!pl.isRobot())
                    {
                        // Store the faceId in the database
                        userFaceUpdate.setInt(1, pl.getFaceId());
                        userFaceUpdate.setString(2, pl.getName());
                        userFaceUpdate.executeUpdate();
                    }
                }
                
                return true;
            }
            catch (SQLException sqlE)
            {
                handleSQLException(sqlE);
            }
        }

        return false;
    }

    /**
     * Saves game scores to the database (both user and games tables)
     *
     * @param ga game to be saved
     *
     * @return true if the save succeeded
     *
     * @throws SQLException if the database isn't available
     */
    public static boolean saveGameScores(SOCGame ga) throws SQLException
    {
        int sGCindex = 1;

        // ensure that the JDBC connection is still valid
        if (checkConnection())
        {
            try
            {
                // fill in the data values to the Prepared statement
                saveGameCommand.setString(sGCindex++, ga.getName());

		// iterate through the players
                for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                {
                    SOCPlayer pl = ga.getPlayer(i);

		    saveGameCommand.setString(sGCindex++, pl.getName());
                }
                for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                {
                    SOCPlayer pl = ga.getPlayer(i);
                    
                    saveGameCommand.setInt(sGCindex++, pl.getTotalVP());
                }
                
                saveGameCommand.setTimestamp(sGCindex++, new Timestamp(ga.getStartTime().getTime()));

                // execute the Command
                saveGameCommand.executeUpdate();

		// iterate through the players
                for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                {
                    SOCPlayer pl = ga.getPlayer(i);
                    int points = pl.getTotalVP();
                    boolean isWinner = points >= 10;
                    
                    // Choose the table to update
                    if (pl.isRobot())
                    {
                        updateRobotStats.setInt(1, (isWinner ? 1 : 0)); // wins
                        updateRobotStats.setInt(2, (isWinner ? 0 : 1)); // losses
                        updateRobotStats.setInt(3, points); // totalpoints
                        updateRobotStats.setString(4, pl.getName());

                        updateRobotStats.executeUpdate();
                    }
                    else // The player is human
                    {
                        updateUserStats.setInt(1, (isWinner ? 1 : 0)); // wins
                        updateUserStats.setInt(2, (isWinner ? 0 : 1)); // losses
                        updateUserStats.setInt(3, points); // totalpoints
                        updateUserStats.setString(4, pl.getName());

                        updateUserStats.executeUpdate();
                    }
                }
                
                return true;
            }
            catch (SQLException sqlE)
            {
                handleSQLException(sqlE);
            }
        }

        return false;
    }

    /**
     * DOCUMENT ME!
     *
     * @param robotName DOCUMENT ME!
     *
     * @return null if robotName not in database
     *
     * @throws SQLException DOCUMENT ME!
     */
    public static SOCRobotParameters retrieveRobotParams(String robotName) throws SQLException
    {
        SOCRobotParameters robotParams = null;

        // ensure that the JDBC connection is still valid
        if (checkConnection())
        {
            try
            {
                // fill in the data values to the Prepared statement
                robotParamsQuery.setString(1, robotName);

                // execute the Query
                ResultSet resultSet = robotParamsQuery.executeQuery();

                // if no results, user is not authenticated
                if (resultSet.next())
                {
                    // retrieve the resultset
                    int mgl = resultSet.getInt(2);
                    int me = resultSet.getInt(3);
                    float ebf = resultSet.getFloat(4);
                    float af = resultSet.getFloat(5);
                    float laf = resultSet.getFloat(6);
                    float dcm = resultSet.getFloat(7);
                    float tm = resultSet.getFloat(8);
                    int st = resultSet.getInt(9);
                    int tf = resultSet.getInt(14);
                    robotParams = new SOCRobotParameters(mgl, me, ebf, af, laf, dcm, tm, st, tf);
                }
                
                resultSet.close();
            }
            catch (SQLException sqlE)
            {
                handleSQLException(sqlE);
            }
        }

        return robotParams;
    }

    /**
     * DOCUMENT ME!
     *
     * @param type either SOCPlayerInfo.HUMAN or SOCPlayerInfo.ROBOT
     *
     * @return array of robot data
     *
     * @throws SQLException DOCUMENT ME!
     */
    public static Vector getStatistics(String type) throws SQLException
    {
        Vector statistics = new Vector();
        Statement stmt = null;
        
        // ensure that the JDBC connection is still valid
        if (checkConnection())
        {
            try
            {
                ResultSet resultSet = null;
                stmt = connection.createStatement();

                // Execute the appropriate query
                if (type.equals(SOCPlayerInfo.ROBOT))
                {
                    resultSet = stmt.executeQuery(ROBOT_STATS_QUERY);
                }
                else // default, even if garbled
                {
                    resultSet = stmt.executeQuery(HUMAN_STATS_QUERY);
                }

                while (resultSet.next())
                {
                    SOCPlayerInfo info = new SOCPlayerInfo();

                    info.setName(resultSet.getString(1));
                    info.setRank(resultSet.getRow());
                    info.setWins(resultSet.getInt(2));
                    info.setLosses(resultSet.getInt(3));
                    info.setTotalPoints(resultSet.getInt(4));
                    info.setAveragePoints(resultSet.getFloat(5));
                    info.setWinRatio(resultSet.getFloat(6));

                    statistics.add(info);
                }
                resultSet.close();
            }
            catch (SQLException sqlE)
            {
                handleSQLException(sqlE);
            }
            finally
            {
                try
                {
                    if (stmt != null)
                        stmt.close();
                }
                catch (SQLException sqlE)
                {
                    handleSQLException(sqlE);
                }
            }
        }
        return statistics;
    }
    
    /**
     * DOCUMENT ME!
     *
     * @param userName for account to be reset
     *
     * @return true if the account was reset, false if not connected
     *
     * @throws SQLException DOCUMENT ME!
     */
    public static boolean resetStatistics(String userName) throws SQLException
    {
        boolean result = false;
        
        // ensure that the JDBC connection is still valid
        if (checkConnection())
        {
            try
            {
                // Server will have authenticated (may be admin)
                
                // Fill in the data values to the Prepared statement
                resetHumanStats.setString(1, userName);
                
                // execute the Command
                resetHumanStats.executeUpdate();

                result = true;
            }
            catch (SQLException sqlE)
            {
                handleSQLException(sqlE);
            }
        }
        return result; // failure only on error
    }

    /**
     * Common behavior for SQL Exceptions.
     */
    protected static void handleSQLException(SQLException x) throws SQLException
    {
        errorCondition = true;
        x.printStackTrace();
        throw x;
    }

    /**
     * DOCUMENT ME!
     */
    public static void cleanup() throws SQLException
    {
        if (checkConnection())
        {
            try
            {
                createAccountCommand.close();
                hostQuery.close();
                lastloginUpdate.close();
                recordLoginCommand.close();
                resetHumanStats.close();
                robotParamsQuery.close();
                saveGameCommand.close();
                updateRobotStats.close();
                updateUserStats.close();
                userFaceQuery.close();
                userPasswordQuery.close();
                connection.close();

                connection = null;
            }
            catch (SQLException sqlE)
            {
                handleSQLException(sqlE);
            }
        }
    }

    /**
     * Useful for debugging. Leave commented out of final build.
     *   /
    private static void dispResultSet(ResultSet rs) throws SQLException
    {
        System.out.println("dispResultSet()");

        int i;

        // used for the column headings
        ResultSetMetaData rsmd = rs.getMetaData();

        // Get the number of columns in the result set
        int numCols = rsmd.getColumnCount();

        // Display column headings
        for (i = 1; i <= numCols; i++)
        {
            if (i > 1)
            {
                System.out.print(",");
            }

            System.out.print(rsmd.getColumnLabel(i));
        }

        System.out.println("");

        // Display data, fetching until end of the result set

        boolean more = rs.next();

        while (more)
        {
            // Loop through each column, getting the
            // column data and displaying
            for (i = 1; i <= numCols; i++)
            {
                if (i > 1)
                {
                    System.out.print(",");
                }

                System.out.print(rs.getString(i));
            }

            System.out.println("");

            // Fetch the next result set row
            more = rs.next();
        }
    }
    */

    /**
     * Constant results for authorization requests.
     */
    public static class Auth
    {
        public static final Auth PASS = new Auth("PASS");
        public static final Auth FAIL = new Auth("FAIL");
        public static final Auth UNKNOWN = new Auth("UNKNOWN");

        private String type;
        /** time to switch to java1.5 code for enums? */
        private Auth(String type)
        {
            this.type = type;
        }
        public String toString()
        {
            return type;
        }
    }
}
