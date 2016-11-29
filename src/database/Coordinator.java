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
    String[] candidates = {"Mary", "Jacinta", "Niamh", "Holly"
    };


    public Coordinator(String name) {
        db = new Database(name);
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
        //if no difference, dont bother
        if (stagingArea.equals(transactionsLog)) return;

        // else find difference
        String newLog = stagingArea;
        String toDo = (stagingArea.replaceFirst(transactionsLog, ""));
        transactionsLog = newLog;
        System.out.println("Adding these votes to db: " + toDo);
        for (String candidate : toDo.split(",")) {
            //TODO make sure not voting for null
            if (Arrays.asList(candidates).contains(candidate)) { //only vote for valid candidates
                 db.voteFor(candidate);
            }
        }
    }

    public String getCurrentResults() {
        return db.printAllRecords();
    }
}
