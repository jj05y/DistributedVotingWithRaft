package jguddi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Created by gary on 11/11/16.
 */
public interface IJguddiService extends Remote {
    void addEndpoint(String ep) throws RemoteException;

    List<String> getEndpoints() throws RemoteException;

    void resetEndpoints(List<String> eps) throws RemoteException;

    void removeEndpoint(String ep) throws  RemoteException;

    String getLeaderEndpoint() throws RemoteException;

    void setLeaderEndpoint(String leaderEndpoint) throws RemoteException;
}
