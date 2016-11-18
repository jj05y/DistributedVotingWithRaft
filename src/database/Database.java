package database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Created by edwinkeville on 09/11/2016.
 */
public class Database {

    private static String DB_FILE_NAME = "raftDB.db";
    private static String TABLE_NAME = "RESULTS";
    Statement stmt = null;
    Connection c = null;

    public Database() {
        deleteTable();
        createTable();
    }


    public void createTable()
    {
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:"+ DB_FILE_NAME +"");

            stmt = c.createStatement();
            String sql = "CREATE TABLE " +TABLE_NAME +
                    "(ID INTEGER PRIMARY KEY     NOT NULL," +
                    " NAME           TEXT    NOT NULL, " +
                    " VOTES          INT     NOT NULL)";
            stmt.executeUpdate(sql);
            stmt.close();
            c.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
    }


    // make boolean
    public void createRecord(String name){
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:"+ DB_FILE_NAME +"");
            c.setAutoCommit(false);

            stmt = c.createStatement();
            String sql = "INSERT INTO "+ TABLE_NAME +" (ID,NAME,VOTES) " +
                        "VALUES (NULL , '" + name + "', 0);";
            stmt.executeUpdate(sql);

            stmt.close();
            c.commit();
            c.close();

        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
    }


    public void voteFor(String name)
    {
        int votes = getVotes(name);

        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:"+ DB_FILE_NAME +"");
            c.setAutoCommit(false);

            stmt = c.createStatement();
            String sql = "UPDATE "+ TABLE_NAME +" set VOTES = "+(votes+1)+" where NAME=\""+name+"\";";
            stmt.executeUpdate(sql);

            stmt.close();
            c.commit();
            c.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
    }

    public int getVotes(String name) {
        int votesCount = 0;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:"+ DB_FILE_NAME +"");
            c.setAutoCommit(false);

            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM "+ TABLE_NAME +" where NAME=\""+name+"\";");
            votesCount = rs.getInt("votes");
            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        return votesCount;
    }


    public String printAllRecords(){
        String allRecords = "";
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:"+ DB_FILE_NAME +"");
            c.setAutoCommit(false);

            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery( "SELECT * FROM "+TABLE_NAME+";" );
            while ( rs.next() ) {
                int id = rs.getInt("id");
                String  name = rs.getString("name");
                int votes  = rs.getInt("votes");

 //               allRecords.concat("ID = "+id + "\n" + "NAME = " + name + "\n" + "VOTES = " + votes + "\n\n");
                System.out.println( allRecords.concat("ID = " + id ));
                allRecords.concat("\n");
                System.out.println( allRecords.concat("NAME = " + name ));
                allRecords.concat("\n");
                System.out.println( allRecords.concat("VOTES = " + votes ));
                allRecords.concat("\n");
                allRecords.concat("\n");
                System.out.println();

            }
            rs.close();
            stmt.close();
            c.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        System.out.println(allRecords);
        return allRecords;
    }

    public void deleteTable(){
        try {
            Class.forName("org.sqlite.JDBC");
            this.c = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE_NAME + "");
            this.stmt = this.c.createStatement();
            String e = "DROP TABLE IF EXISTS "+TABLE_NAME;
            this.stmt.executeUpdate(e);
            this.stmt.close();
            this.c.close();
        } catch (Exception var2) {
            System.err.println(var2.getClass().getName() + ": " + var2.getMessage());
            System.exit(0);
        }
    }

    public void deleteDB(){
        File file = new File(DB_FILE_NAME);
        file.delete();
    }


    public static void main (String[] args) {
        //test main
        Database db = new Database();
        db.createRecord("semannnt");
        db.createRecord("hoops");
        db.createRecord("boops");
        db.voteFor("hoops");
        db.voteFor("hoops");
        db.voteFor("hoops");
        db.voteFor("hoops");
        System.out.println("sandy now has: "+db.getVotes("hoops"));
        System.out.println("anne now has: "+db.getVotes("boops"));
        db.printAllRecords();

    }

}
