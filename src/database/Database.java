package database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by edwinkeville on 09/11/2016.
 */
public class Database {

    private static String DB_FILE_NAME = "raftDB.db";
    List<String> nameList = null;
    Statement stmt = null;
    Connection c = null;

    public Database() {
        nameList = new LinkedList<String>();
        createTable();
    }


    public void createTable()
    {
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:"+ DB_FILE_NAME +"");

            stmt = c.createStatement();
            String sql = "CREATE TABLE RESULTS " +
                    "(ID INT PRIMARY KEY     NOT NULL," +
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



    public void voteFor(String name){
            if (nameList.contains(name)) {
                updateRecord(name);
            }else{
                nameList.add(name);
                createRecord(name);
            }
    }


    public void createRecord(String name){
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:"+ DB_FILE_NAME +"");
            c.setAutoCommit(false);

            stmt = c.createStatement();
            String sql = "INSERT INTO RESULTS (ID,NAME,VOTES) " +
                        "VALUES ("+ nameList.size() +", '" + name + "', 1);";
            stmt.executeUpdate(sql);

            stmt.close();
            c.commit();
            c.close();

        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
    }


    public void updateRecord(String name)
    {
        int votes = getVotes(name);

        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:"+ DB_FILE_NAME +"");
            c.setAutoCommit(false);

            int id = nameList.indexOf(name)+1;
            stmt = c.createStatement();
            String sql = "UPDATE RESULTS set VOTES = "+(votes+1)+" where ID="+id+";";
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
        int id = nameList.indexOf(name)+1;
        int votesCount = 0;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:"+ DB_FILE_NAME +"");
            c.setAutoCommit(false);

            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM RESULTS where ID=" + id + ";");
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


    public void printAllRecords(){
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:"+ DB_FILE_NAME +"");
            c.setAutoCommit(false);

            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery( "SELECT * FROM RESULTS;" );
            while ( rs.next() ) {
                int id = rs.getInt("id");
                String  name = rs.getString("name");
                int votes  = rs.getInt("votes");
                System.out.println( "ID = " + id );
                System.out.println( "NAME = " + name );
                System.out.println( "VOTES = " + votes );
                System.out.println();
            }
            rs.close();
            stmt.close();
            c.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
    }


    public static void main (String[] args) {
        //test main
        Database db = new Database();
        db.voteFor("sandy");
        db.voteFor("anne");
        db.voteFor("sandy");
        db.printAllRecords();
        db.voteFor("sandy");
        db.voteFor("anne");
        db.voteFor("sandy");
        db.printAllRecords();
        db.voteFor("sandy");
        db.voteFor("anne");
        db.voteFor("sandy");
        System.out.println("sandy now has: "+db.getVotes("sandy"));
        System.out.println("anne now has: "+db.getVotes("anne"));



        //deleting db file after testing.
        File file = new File(DB_FILE_NAME);
        file.delete();

    }

}
