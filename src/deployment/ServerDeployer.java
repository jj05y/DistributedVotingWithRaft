package deployment;

import jguddi.IJguddiService;
import server.IServer;
import server.Server;

import javax.xml.ws.Endpoint;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;

/**
 * Created by gary on 11/11/16.
 */
public class ServerDeployer {

    public ServerDeployer() {
    }

    public static void main(String[] args) {
        Random portRandomise = new Random();
        String name = "Ted" + portRandomise.nextInt(100);

        int port = portRandomise.nextInt(20000) + 30000;
        String endpoint = "http://localhost:" + port + "/RaftServer/" + name;
        Endpoint.publish(endpoint, new Server(name));
        System.out.println("Published " + name + " on " + endpoint);


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

        //bind jguddi to the registy

        IServer server = null;
        try {
            server = (IServer) UnicastRemoteObject.exportObject(new Server(name), 0);
            registry.bind("jguddi", server);

        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (AlreadyBoundException e) {

        }

        //get jguddi and add the endpoint
        try {
            if (registry != null) {
                for (String serviceTag : registry.list()) {
                    System.out.println("Service from rmi registry " + serviceTag);
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
