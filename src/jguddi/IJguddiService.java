package jguddi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Created by gary on 11/11/16.
 */
public interface IJguddiService extends Remote {
    void addEndpoint(Endpoint ep) throws RemoteException;

    List<Endpoint> getEndpoints() throws RemoteException;

    void resetEndpoints(List<Endpoint> eps) throws RemoteException;
}
