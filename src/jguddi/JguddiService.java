package jguddi;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Vector;

/**
 * Created by gary on 11/11/16.
 */
public class JguddiService implements IJguddiService {
    private List<String> endpoints;
    private String leaderEndpoint;

    public JguddiService() {
        this.endpoints = new Vector<>();
    }

    @Override
    public void addEndpoint(String ep) {
        this.endpoints.add(ep);
    }

    @Override
    public List<String> getEndpoints() {
        return new Vector<>(endpoints);
    }

    @Override
    public String getLeaderEndpoint() throws RemoteException {
        return leaderEndpoint;
    }

    @Override
    public void setLeaderEndpoint(String leaderEndpoint) throws RemoteException {
        this.leaderEndpoint = leaderEndpoint;
    }

    @Override
    public void resetEndpoints(List<String> eps) {
        endpoints = eps;
    }

    @Override
    public void removeEndpoint(String ep) {
        synchronized (endpoints) {
            int indexOfDeadEp = endpoints.indexOf(ep);
            if (indexOfDeadEp != -1) {
                endpoints.remove(endpoints.indexOf(ep));
            }
        }
    }
}
