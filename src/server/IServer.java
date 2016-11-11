package server;

import javax.jws.WebService;

/**
 * Created by gary on 11/11/16.
 */
@WebService(name="IServer")
public interface IServer {
    int requestVote(int term);

    String recieveHeartBeat(Object o);
}
