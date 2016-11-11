package jguddi;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Created by gary on 11/11/16.
 */
public class JguddiServer implements Runnable {
    private Registry registry;

    public JguddiServer() {
        registry = null;
    }

    @Override
    public void run() {
        try {
            registry = LocateRegistry.createRegistry(1099);
        } catch(RemoteException re) {
            try {
                registry = LocateRegistry.getRegistry();
            } catch( RemoteException e) {
                System.out.println("Failure getting registry::");
            }

        }
        try {
            IJguddiService service = (IJguddiService) UnicastRemoteObject.exportObject(new JguddiService(), 0);
            if (registry != null) {
                registry.bind("jguddi", service);
            }
        } catch (RemoteException e) {

        } catch(AlreadyBoundException abe) {

        }
    }
}
