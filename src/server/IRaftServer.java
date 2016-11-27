package server;

import javax.jws.WebService;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by gary on 11/11/16.
 */
@WebService(name="IRaftServer")
public interface IRaftServer {
    int requestVote(int term, String name);

    String recieveHeartBeat(String data, String sentBy);

    void commitStagingArea();

    void castVote(String name);

    String getCurrentVotes();
}
