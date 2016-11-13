package server;

import javax.jws.WebService;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by gary on 11/11/16.
 */
@WebService(name="IServer")
public interface IServer {
    int requestVote(int term, String name);

    String recieveHeartBeat(Object o, String sentBy);
}
