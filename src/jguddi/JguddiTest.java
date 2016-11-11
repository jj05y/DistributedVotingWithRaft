package jguddi;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Created by gary on 11/11/16.
 */
public class JguddiTest {

    private static Registry registry;

    public static void main(String[] args) {
        Thread myThread = new Thread(new JguddiServer());
        System.out.println("Starting Jguddi Server::");
        myThread.start();

        try {
            registry = LocateRegistry.getRegistry();
        } catch (RemoteException e) {
            System.out.println("Failure getting registry from test::");
        }

        try {
            if (registry != null) {
                for (String serviceTag : registry.list()) {
                    IJguddiService jguddiService = (IJguddiService) registry.lookup(serviceTag);
                    System.out.println(serviceTag);
                    jguddiService.addEndpoint(new Endpoint("Hi", "Http://hotpotato", "bollox",""));
                    System.out.println(jguddiService.getEndpoints().size());
                    for (Endpoint ep : jguddiService.getEndpoints()) {
                        System.out.println(ep);
                    }
                }
            }
        } catch (RemoteException re) {
            re.printStackTrace();
        } catch (NotBoundException nbe) {
            nbe.printStackTrace();
        }
    }
}