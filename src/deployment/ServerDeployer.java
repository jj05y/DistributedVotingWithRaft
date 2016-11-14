package deployment;

import jguddi.IJguddiService;
import jguddi.JguddiService;
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

    static IJguddiService staticService;

    public static void main(String[] args) {
        Random rand = new Random();

        //Get the server name from arguments
        String name;
        if (args.length == 1) {
            name = args[0];
        } else {
            name = "Server" + rand.nextInt(10000);
        }

        int port = rand.nextInt(20000) + 30000; //Assign random port to the server
        String endpoint = "http://localhost:" + port + "/RaftServer/" + name;
        Endpoint.publish(endpoint, new Server(name));
        System.out.println("Published " + name + " on " + endpoint);


        Registry registry = null;
        try {
            registry = LocateRegistry.createRegistry(1099);
            //bind jguddi to the registy
            IJguddiService jguddiServer = null;
            try {
                if (registry!= null) {
                    staticService = new JguddiService();
                    jguddiServer = (IJguddiService) UnicastRemoteObject.exportObject(staticService, 0);
                    if (jguddiServer != null) registry.bind("jguddi", jguddiServer);
                    System.out.println("bound jguddi");
                }else {
                    System.out.println("registry null");
                }
            } catch (RemoteException e) {
                System.out.println("remote exception from deployer");
                e.printStackTrace();
            } catch (AlreadyBoundException e) {
                System.out.println("alreaedy bound in deployer");
            }


        } catch (RemoteException e) {
            try {
                registry = LocateRegistry.getRegistry();
            } catch (RemoteException e1) {
                System.out.println("WARNING: Failed to get or create RMI Registry");
                System.exit(0);
            }
        }

        //get jguddi and add the endpoint
        try {
            if (registry != null) {
                for (String serviceTag : registry.list()) {
                    System.out.println("Service from rmi registry: " + serviceTag);
                    IJguddiService jguddiService = (IJguddiService) registry.lookup(serviceTag);
                    jguddiService.addEndpoint(endpoint);
                }
            }
        } catch (RemoteException re) {
            System.out.println("remote excetpion from adding end point");
            re.printStackTrace();
        } catch (NotBoundException nbe) {
            System.out.println("not bound exception");
            //nbe.printStackTrace();
        }


    }
}
