package jguddi;

import java.util.List;
import java.util.Vector;

/**
 * Created by gary on 11/11/16.
 */
public class JguddiService implements IJguddiService {
    private List<Endpoint> endpoints;

    public JguddiService() {
        this.endpoints = new Vector<>();
    }

    @Override
    public void addEndpoint(Endpoint ep) {
        this.endpoints.add(ep);
    }

    @Override
    public List<Endpoint> getEndpoints() {
        return endpoints;
    }

    @Override
    public void resetEndpoints(List<Endpoint> eps) {
        endpoints = eps;
    }
}
