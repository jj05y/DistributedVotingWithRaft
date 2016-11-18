package database;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.jar.Pack200;

/**
 * Created by joe on 18/11/16.
 */
public class Coordinator {

    //bob,ted.ted.bob
    String transactionsLog; //123
    String stagingArea; //12345
    Database db;
    String[] candidates = {"Mary, Jacinta, Niamh, Holly"};


    public Coordinator() {
        db = new Database();
        for (String candidate : candidates) {
            db.createRecord(candidate);
        }

        transactionsLog = "";
        stagingArea = "";
    }

    public void addToStagingArea(String toBeStaged) {
        stagingArea = toBeStaged;
    }

    public String getLog() {
        return transactionsLog;
    }

    public void commitStagingArea() {
        //find difference
        String newLog = stagingArea;
        String toDo = (stagingArea.replace(transactionsLog, ""));
        transactionsLog = newLog;
        for (String candiddate : toDo.split(",")) {
            db.voteFor(candiddate);
        }
    }

    //public String getVotingResults() {
       // return db.printAllRecords();
    //}
}
