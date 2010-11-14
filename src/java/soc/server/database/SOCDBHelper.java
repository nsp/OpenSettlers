/**
 * Open Settlers - an open implementation of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 * Portions of this file Copyright (C) 2009-2010 Jeremy D Monin <jeremy@nand.net>
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. **/
package soc.server.database;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Properties;
import java.util.Vector;

import soc.game.Game;
import soc.game.Player;
import soc.util.PlayerInfo;
import soc.util.RobotParameters;


/**
 * This class contains methods for connecting to a database
 * and for manipulating the data stored there.
 *
 * Based on jdbc code found at www.javaworld.com
 *
 * @author Robert S. Thomas
 */
/**
 * This code assumes that you're using mySQL as your database,
 * but allows you to use other database types.
 * The default URL is "jdbc:mysql://localhost/socdata".
 * The default driver is "com.mysql.jdbc.Driver".
 * These can be changed by supplying properties to {@link #initialize(String, String, Properties)}
 * for {@link #PROP_OPENSETTLERS_DB_URL} and {@link #PROP_OPENSETTLERS_DB_DRIVER}.
 *<P>
 * It uses a database created with the following commands:
 *<code>
 * CREATE DATABASE socdata;
 * USE socdata;
 * CREATE TABLE users (nickname VARCHAR(20), host VARCHAR(50), password VARCHAR(20), email VARCHAR(50), lastlogin DATE, wins INT, losses INT, face INT, totalpoints INT);
 * CREATE TABLE logins (nickname VARCHAR(20), host VARCHAR(50), lastlogin DATE);
 * CREATE TABLE games (gamename VARCHAR(20), player1 VARCHAR(20), player2 VARCHAR(20), player3 VARCHAR(20), player4 VARCHAR(20), score1 TINYINT, score2 TINYINT, score3 TINYINT, score4 TINYINT, starttime TIMESTAMP, endtime TIMESTAMP);
 * CREATE TABLE robotparams (robotname VARCHAR(20), maxgamelength INT, maxeta INT, etabonusfactor FLOAT, adversarialfactor FLOAT, leaderadversarialfactor FLOAT, devcardmultiplier FLOAT, threatmultiplier FLOAT, strategytype INT, starttime TIMESTAMP, endtime TIMESTAMP, gameswon INT, gameslost INT, tradeFlag BOOL, wins INT, losses INT, face INT, totalpoints INT);
 *</code>
 */
public class SOCDBHelper
{
    // If a new property is added, please add a PROP_OPENSETTLERS_DB_ constant
    // and also add it to SOCServer.PROPS_LIST.

    /** Property to specify the SQL database server. 
	 * @since 1.1.10
	 */
    public static final String PROP_OPENSETTLERS_DB_ENABLED = "osettlers.db.enabled";

	/** Property <tt>osettlers.db.user</tt> to specify the server's SQL database username.
     * @since 1.1.09
     */
    public static final String PROP_OPENSETTLERS_DB_USER = "osettlers.db.user";

    /** Property <tt>osettlers.db.pass</tt> to specify the server's SQL database password.
     * @since 1.1.09
     */
    public static final String PROP_OPENSETTLERS_DB_PASS = "osettlers.db.pass";

    /** Property <tt>osettlers.db.driver</tt> to specify the server's JDBC driver class.
     * The default driver is "com.mysql.jdbc.Driver".
     * If the {@link #PROP_OPENSETTLERS_DB_URL URL} begins with "jdbc:postgresql:",
     * the driver will be "org.postgresql.Driver".
     * If the <tt>URL</tt> begins with "jdbc:sqlite:",
     * the driver will be "org.sqlite.JDBC".
     * @since 1.1.09
     */
    public static final String PROP_OPENSETTLERS_DB_DRIVER = "osettlers.db.driver";

    /** Property <tt>osettlers.db.url</tt> to specify the server's URL.
     * The default URL is "jdbc:mysql://localhost/socdata".
     * @since 1.1.09
     */
    public static final String PROP_OPENSETTLERS_DB_URL = "osettlers.db.url";

    public static Connection connection = null;

    /**
     * Retain the URL (default, or passed via props to {@link #initialize(String, String, Properties)}).
     * @since 1.1.09
     */
    private static String dbURL = null;

    /** Cached url used when reconnecting on error */
    private static String url;
	
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

    private static String CREATE_ACCOUNT_COMMAND =  "INSERT INTO users VALUES (?,?,?,?,?,?,?,?,?);";

    private static String RECORD_LOGIN_COMMAND =    "INSERT INTO logins VALUES (?,?,?);";

    private static String USER_PASSWORD_QUERY =     "SELECT password FROM users WHERE ( users.nickname = ? );";
	
    private static String HOST_QUERY =              "SELECT nickname FROM users WHERE ( users.host = ? );";

    private static String LASTLOGIN_UPDATE =        "UPDATE users SET lastlogin = ? WHERE nickname = ? ;";

    private static String SAVE_GAME_COMMAND =       "INSERT INTO games VALUES (?,?,?,?,?,?,?,?,?,?,?);";
	
    private static String ROBOT_PARAMS_QUERY =      "SELECT * FROM robotparams WHERE robotname = ?;";
    
    private static String ROBOT_STATS_QUERY =       "SELECT robotname, wins, losses, totalpoints, totalpoints/(wins+losses) AS avg, (100*(wins/(wins+losses))) AS pct FROM robotparams WHERE (wins+losses) > 0 ORDER BY pct desc, avg desc, totalpoints desc;";
    
    private static String HUMAN_STATS_QUERY =       "SELECT  nickname, wins, losses, totalpoints, totalpoints/(wins+losses) AS avg, (100*(wins/(wins+losses))) AS pct FROM       users WHERE (wins+losses) > 0 ORDER BY pct desc, avg desc, totalpoints desc;";

    private static String RESET_HUMAN_STATS =       "UPDATE users SET wins = 0, losses = 0, totalpoints = 0 WHERE nickname = ?;";

    private static String UPDATE_ROBOT_STATS =      "UPDATE robotparams SET wins = wins + ?, losses = losses + ?, totalpoints = totalpoints + ? WHERE robotname = ?;";
    
    private static String UPDATE_USER_STATS =       "UPDATE users SET wins = wins + ?, losses = losses + ?, totalpoints = totalpoints + ? WHERE nickname = ?;";
    
    private static String USER_FACE_QUERY =         "SELECT face FROM users WHERE users.nickname = ?;";
    
    private static String USER_FACE_UPDATE =        "UPDATE users SET face = ? WHERE nickname = ?;";
    
    private static PreparedStatement createAccountCommand = null;
    private static PreparedStatement recordLoginCommand = null;
    private static PreparedStatement userPasswordQuery = null;
    private static PreparedStatement hostQuery = null;
    private static PreparedStatement lastloginUpdate = null;
    private static PreparedStatement saveGameCommand = null;
    private static PreparedStatement robotParamsQuery = null;
    private static PreparedStatement resetHumanStats = null;
    private static PreparedStatement updateRobotStats = null;
    private static PreparedStatement updateUserStats = null;
    private static PreparedStatement userFaceQuery = null;
    private static PreparedStatement userFaceUpdate = null;

    /**
     * This makes a connection to the database
     * and initializes the prepared statements.
     *<P>
     * The default URL is "jdbc:mysql://localhost/socdata".
     * The default driver is "com.mysql.jdbc.Driver".
     * These can be changed by supplying <code>props</code>.
     * If props = null, we behave as though db.enabled=false;
     *
     * @param props  null, or properties containing {@link #PROP_OPENSETTLERS_DB_USER},
     *       {@link #PROP_OPENSETTLERS_DB_URL}, and any other desired properties.
     * @throws SQLException if an SQL command fails, or the db couldn't be
     *         initialized;
     *         or if the {@link #PROP_OPENSETTLERS_DB_DRIVER} property is not mysql, not sqlite, not postgres,
     *         but the {@link #PROP_OPENSETTLERS_DB_URL} property is not provided.
     */
    public static boolean initialize(Properties props) throws SQLException
    {
        if (isConnected())
            throw new IllegalStateException("Database already initialized");

    	String driverclass = "com.mysql.jdbc.Driver";
    	dbURL = "jdbc:mysql://localhost/socdata";
        String prop_dbURL = props.getProperty(PROP_OPENSETTLERS_DB_URL);
        String prop_driverclass = props.getProperty(PROP_OPENSETTLERS_DB_DRIVER);
        if (prop_dbURL != null)
        {
            dbURL = prop_dbURL;
            if (prop_driverclass != null)
                driverclass = prop_driverclass;
            else if (prop_dbURL.startsWith("jdbc:postgresql"))
                driverclass = "org.postgresql.Driver";
            else if (prop_dbURL.startsWith("jdbc:sqlite:"))
                driverclass = "org.sqlite.JDBC";
            else if (! prop_dbURL.startsWith("jdbc:mysql"))
            {
                throw new SQLException("JDBC: URL property is set, but driver property is not: ("
                    + PROP_OPENSETTLERS_DB_URL + ", " + PROP_OPENSETTLERS_DB_DRIVER + ")");
            }
        } else {
            if (prop_driverclass != null)
                driverclass = prop_driverclass;
            // if it's mysql, use the mysql default url above.
            // if it's postgres or sqlite, use that.
            // otherwise, not sure what they have.
            if (driverclass.contains("postgresql"))
            {
                dbURL = "jdbc:postgresql://localhost/socdata";
            }
            else if (driverclass.contains("sqlite"))
            {
                dbURL = "jdbc:sqlite:socdata.sqlite";
            }
            else if (! driverclass.contains("mysql"))
            {
                throw new SQLException("JDBC: Driver property is set, but URL property is not: ("
                    + PROP_OPENSETTLERS_DB_DRIVER + ", " + PROP_OPENSETTLERS_DB_URL + ")");
            }
//            props.setProperty(PROP, value)
    	}
        userName = props.getProperty(PROP_OPENSETTLERS_DB_USER);
        password = props.getProperty(PROP_OPENSETTLERS_DB_PASS);
        url = props.getProperty(PROP_OPENSETTLERS_DB_URL);
        String driver = props.getProperty(PROP_OPENSETTLERS_DB_DRIVER);

    	try
        {
            // Load the JDBC driver. Revisit exceptions when /any/ JDBC allowed.
            Class.forName(driverclass).newInstance();
            connect();
        }
        catch (ClassNotFoundException x)
        {
            SQLException sx =
                new SQLException("JDBC driver is unavailable: " + driverclass);
            sx.initCause(x);
            throw sx;
        }
        catch (Throwable x) // everything else
        {
            // InstantiationException & IllegalAccessException
            // should not be possible  for org.gjt.mm.mysql.Driver
            // ClassNotFound
            SQLException sx = new SQLException("Unable to initialize user database: " + url);
            sx.initCause(x);
            throw sx;
        }
        
        return isConnected();
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
        connection = DriverManager.getConnection(dbURL, userName, password);

        errorCondition = false;
        
        // prepare PreparedStatements for queries
        createAccountCommand = connection.prepareStatement(CREATE_ACCOUNT_COMMAND);
        recordLoginCommand = connection.prepareStatement(RECORD_LOGIN_COMMAND);
        userPasswordQuery = connection.prepareStatement(USER_PASSWORD_QUERY);
        hostQuery = connection.prepareStatement(HOST_QUERY);
        lastloginUpdate = connection.prepareStatement(LASTLOGIN_UPDATE);
        saveGameCommand = connection.prepareStatement(SAVE_GAME_COMMAND);
        robotParamsQuery = connection.prepareStatement(ROBOT_PARAMS_QUERY);

        resetHumanStats = connection.prepareStatement(RESET_HUMAN_STATS);
        userFaceQuery = connection.prepareStatement(USER_FACE_QUERY);
        userFaceUpdate = connection.prepareStatement(USER_FACE_UPDATE);
        updateRobotStats = connection.prepareStatement(UPDATE_ROBOT_STATS);
        updateUserStats = connection.prepareStatement(UPDATE_USER_STATS);
        
        return true;
    }
    
    /**
     * Retrieve this user's password from the database.
     *
     * @param sUserName Username who needs password
     *
     * @return null if user account doesn't exist, or if no database is currently connected
     *
     * @throws SQLException if any unexpected database problem
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
    public static boolean saveFaces(Game ga) throws SQLException
    {
        // Insure that the JDBC connection is still valid
        if (checkConnection())
        {
            try
            {
                // Record face for humans
                for (int i = 0; i < ga.maxPlayers; i++)
                {
                    Player pl = ga.getPlayer(i);
                    
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
    public static boolean saveGameScores(Game ga) throws SQLException
    {
        int sGCindex = 1;
        // TODO 6-player: save their scores too, if
        // those fields are in the database.

        // ensure that the JDBC connection is still valid
        if (checkConnection())
        {
            try
            {
                // fill in the data values to the Prepared statement
                saveGameCommand.setString(sGCindex++, ga.getName());

                // iterate through the players
                for (int i = 0; i < ga.maxPlayers; i++)
                {
                    Player pl = ga.getPlayer(i);

	                saveGameCommand.setString(sGCindex++, pl.getName());
                }
                for (int i = 0; i < ga.maxPlayers; i++)
                {
                    Player pl = ga.getPlayer(i);
                    
                    saveGameCommand.setInt(sGCindex++, pl.getTotalVP());
                }
                
                saveGameCommand.setTimestamp(sGCindex++, new Timestamp(ga.getStartTime().getTime()));
                saveGameCommand.setTimestamp(sGCindex++, new Timestamp(System.currentTimeMillis()));

                // execute the Command
                saveGameCommand.executeUpdate();

                // iterate through the players
                for (int i = 0; i < ga.maxPlayers; i++)
                {
                    Player pl = ga.getPlayer(i);
                    int points = pl.getTotalVP();
                    boolean isWinner = points >= 10;
                    
                    // Choose the table to update
                    if (pl.isRobot())
                    {
                        updateRobotStats.setInt(1, (isWinner ? 1 : 0)); // wins
                        updateRobotStats.setInt(2, (isWinner ? 0 : 1)); // losses
                        updateRobotStats.setInt(3, points); // totalpoints
                        updateRobotStats.setString(4, pl.getName());

//                        updateRobotStats.executeUpdate();
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
    public static RobotParameters retrieveRobotParams(String robotName) throws SQLException
    {
        RobotParameters robotParams = null;

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
                    robotParams = new RobotParameters(mgl, me, ebf, af, laf, dcm, tm, st, tf);
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
     * @param type either PlayerInfo.HUMAN or PlayerInfo.ROBOT
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
                if (type.equals(PlayerInfo.ROBOT))
                {
                    resultSet = stmt.executeQuery(ROBOT_STATS_QUERY);
                }
                else // default, even if garbled
                {
                    resultSet = stmt.executeQuery(HUMAN_STATS_QUERY);
                }

                while (resultSet.next())
                {
                    PlayerInfo info = new PlayerInfo();

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
     * DOCUMENT ME!
     */
    public static void cleanup() throws SQLException
    {
        if (checkConnection())
        {
            try
            {
                createAccountCommand.close();
                userPasswordQuery.close();
                hostQuery.close();
                lastloginUpdate.close();
                saveGameCommand.close();
                robotParamsQuery.close();
                connection.close();
                updateRobotStats.close();
                updateUserStats.close();
                userFaceQuery.close();
                recordLoginCommand.close();
                resetHumanStats.close();
                connection = null;
            }
            catch (SQLException sqlE)
            {
                handleSQLException(sqlE);
            }
        }
    }

    //-------------------------------------------------------------------
    // dispResultSet
    // Displays all columns and rows in the given result set
    //-------------------------------------------------------------------
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
