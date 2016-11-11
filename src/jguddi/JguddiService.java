package jguddi;

import java.util.List;
import java.util.Vector;

/**
 * Created by gary on 11/11/16.
 */
public class JguddiService implements IJguddiService {
    private List<String> endpoints;

    public JguddiService() {
        this.endpoints = new Vector<>();
    }

    @Override
    public void addEndpoint(String ep) {
        this.endpoints.add(ep);
    }

    @Override
    public List<String> getEndpoints() {
        return endpoints;
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
