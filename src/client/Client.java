package client;

import jguddi.IJguddiService;
import server.IRaftServer;
import server.IVotingService;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Created by joe on 27/11/16.
 */
public class Client {


    public static void main(String[] args) {

        String[] validInputs = {"Mary", "Jacinta", "Niamh", "Holly", "print"};

        Scanner in = new Scanner(System.in);
        String input;

        while (!(input = in.nextLine()).equals("quit")) {

            System.out.println((!input.equals("print"))?"Client attempting to vote for: " + input:"Client Requesting current votes");

            if (Arrays.asList(validInputs).contains(input)) {
                Registry registry;
                IRaftServer raftServer = null;
                IJguddiService jguddiService = null;

                try {
                    registry = LocateRegistry.getRegistry();
                    for (String service : registry.list()) {
                        jguddiService = (IJguddiService) registry.lookup(service);
                    }
                } catch (RemoteException e) {
                    System.err.println("No registry available to client");
                    System.exit(1);
                } catch (NotBoundException e) {
                    System.err.println("Jguddi not bound");
                    System.exit(1);
                }

                if (jguddiService != null) {
                    try {
                        String leaderEndpoint = jguddiService.getLeaderEndpoint();
                        String url = leaderEndpoint + "?wsdl";
                        String qname = "http://server/";
                        String qname2 = "ServerService";
                        Service service = Service.create(new URL(url), new QName(qname, qname2));
                        System.out.println("leader endpoint: " + url);
                        raftServer = service.getPort(IRaftServer.class);
                    } catch (WebServiceException wse) {
                        System.err.println("Can't get leader endpoint :(");
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }

                if (raftServer != null) {
                    if (input.equals("print")) {
                        System.out.println("Current Stats: \n" + raftServer.getCurrentVotes());
                    } else {
                        raftServer.castVote(input);
                        System.out.println("vote cast for " + input);
                    }
                }
            } else {
                System.out.println(input + " is not a valid candidate or print option");
            }


        }
    }
}
