package deployment;

import jguddi.IJguddiService;
import server.Server;

import javax.xml.ws.Endpoint;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;

/**
 * Created by gary on 11/11/16.
 */
public class ServerDeployer {
    public ServerDeployer() {
    }

    public static void main(String[] args) {
        String name = "Ted";
        Random portRandomise = new Random();
        int port = portRandomise.nextInt(20000) + 30000;
        String endpoint = "http://localhost:" + port + "/RaftServer/" + name;
        Endpoint.publish(endpoint, new Server(name));

        Registry registry = null;
        try {
            registry = LocateRegistry.createRegistry(1099);
        } catch (RemoteException e) {
            try {
                registry = LocateRegistry.getRegistry();
            } catch (RemoteException e1) {
                System.out.println("WARNING: Failed to get or create RMI Registry");
                System.exit(0);
            }
        }
        try {
            if (registry != null) {
                for (String serviceTag : registry.list()) {
                    IJguddiService jguddiService = (IJguddiService) registry.lookup(serviceTag);
                    String publishedTo = endpoint;
                    String url = endpoint + "?wsdl";
                    String qname = "http://server/";
                    String qname2 = "ServerService";
                    jguddiService.addEndpoint(new jguddi.Endpoint(publishedTo, url, qname, qname2));

                }
            }
        } catch (RemoteException re) {
            re.printStackTrace();
        } catch (NotBoundException nbe) {
            nbe.printStackTrace();
        }
    }
}
