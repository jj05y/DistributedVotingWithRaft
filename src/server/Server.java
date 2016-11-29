package server;

import com.sun.xml.internal.ws.client.ClientTransportException;
import database.Coordinator;
import jguddi.IJguddiService;
import jguddi.JguddiService;

import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Random;
import java.util.Vector;

/**
 * Created by joe on 05/11/16.
 */

@WebService(name = "IRaftServer")
public class Server implements IElectionTimerCallBack, IHeartBeatCallBack, IRaftServer, IOtherServerCheckerCallBack, IVotingService {
    public static final int OTHER_SERVER_CHECK_PAUSE = 1000;
    public static final int RAND_ELEC_TIME = 1500;
    private static int PULSE = 500;

    private List<IRaftServer> servers;      //Other servers on Jguddi registry
    private List<String> serverEndpoints;
    private String leaderEndpoint;
    private String myEndpoint;

    private String latestVotes;


    private enum State {CANDIDATE, LEADER, FOLLOWER}

    private State state;
    private Thread electionTimer;
    private Thread heartBeat;
    private int term;
    private String name;
    private Coordinator coordinator;
    static IJguddiService staticService;


    public Server() {
    }

    public Server(String name, String myEndpoint) {
        this.name = name;
        this.myEndpoint = myEndpoint;
        servers = new Vector<>();
        serverEndpoints = new Vector<>();
        latestVotes = "";
        coordinator = new Coordinator(name);
        getServersFromJguddi();
        (new Thread(new OtherServerChecker(this))).start();
        startServer();
    }

    public void getServersFromJguddi() {

        java.rmi.registry.Registry registry = null;
        try {
            registry = LocateRegistry.getRegistry();
            try {
                registry.list();
            } catch (RemoteException re) {
                System.err.println(name + ": " + "The registry has died :/ going to make step up and be in charge of it");

                //NOW have to create a new instance of jguddi and repopulate it with things
                try {
                    registry = LocateRegistry.createRegistry(1099);

                    //bind jguddi to the new registy
                    IJguddiService jguddiServer = null;
                    try {
                        if (registry != null) {
                            staticService = new JguddiService();
                            jguddiServer = (IJguddiService) UnicastRemoteObject.exportObject(staticService, 0);
                            if (jguddiServer != null) registry.bind("jguddi", jguddiServer);
                            System.out.println(name + ": bound new jguddi service");

                            //Now need to pop in all servers known to me at this time
                            // I'm not the leader, but i saved the day!
                            for (String serverEndpoint : serverEndpoints) {
                                staticService.addEndpoint(serverEndpoint);
                            }

                            staticService.setLeaderEndpoint(leaderEndpoint);
                            System.out.println(name + ": told jguddi who the leader is");
                        } else {
                            System.err.println("registry null");
                        }
                    } catch (RemoteException e) {
                        System.err.println("remote exception from deployer");
                        e.printStackTrace();
                    } catch (AlreadyBoundException e) {
                        System.err.println("alreaedy bound in deployer");
                    }
                } catch (RemoteException re2) {
                    System.err.println(name + ": failed at restarting jguddi");
                }
            }
        } catch (RemoteException re3) {
            System.err.println(name + ": TOTALLY FAILED at gjuddi");
        }
        //now registry is set
        try {   //TODO: when there's only one server this will constantly spew remoteExceptions, handle?
            IJguddiService jguddi = null;
            for (String service : registry.list()) {
                jguddi = (IJguddiService) registry.lookup(service);
            }
            if (jguddi != null) {
                serverEndpoints = jguddi.getEndpoints();
                leaderEndpoint = jguddi.getLeaderEndpoint();
            }
        } catch (RemoteException re) {
            System.err.println("remote exception in getting jguddi");
            //  re.printStackTrace();
        } catch (NotBoundException ne) {
            System.err.println("Service Not Bound, server is dead");
        }
        //get the list of servers from the list of end points
        servers.clear();
        try {
            Vector<String> deadEndpoints = new Vector<>();
            for (String ep : serverEndpoints) {
                try {
                    String url = ep + "?wsdl";
                    String qname = "http://server/";
                    String qname2 = "ServerService";
                    Service service = Service.create(new URL(url), new QName(qname, qname2));
                    servers.add(service.getPort(IRaftServer.class));
                } catch (WebServiceException wse) {
                    System.err.println("Web service no longer exists, must be dead");
                    deadEndpoints.add(ep);
                }
            }

            //clean up
            try {
                IJguddiService jguddi = null;
                for (String service : registry.list()) {
                    jguddi = (IJguddiService) registry.lookup(service);
                }
                for (String deadEnpoint : deadEndpoints) {
                    System.out.println("Removing Dead Endpoint " + deadEnpoint);
                    if (jguddi != null) jguddi.removeEndpoint(deadEnpoint);
                }
            } catch (RemoteException re) {
                System.err.println("remote exception in clearing dead enpoints");
            } catch (NotBoundException ne) {
                System.err.println("Service Not Bound, server is dead");
            }

        } catch (MalformedURLException mue) {
            System.err.println("Bad Url::");
        }
        System.out.println("There are now " + servers.size() + " server(s)");
    }


    public void startServer() {
        state = State.FOLLOWER;
        electionTimer = new Thread(new ElectionTimer(this));
        electionTimer.start();
    }


    @Override
    public void electionTimerUp() {
        System.out.println("Term: " + term + "\t" + name + ": Election timer up");
        //request votes

        state = State.CANDIDATE;
        System.out.println("Term: " + term + "\t" + name + ": is now candidate");
        //new term!
        term++;
        int numVotes = 0;
        synchronized (servers) {
            for (IRaftServer s : servers) {
                try {
                    numVotes += s.requestVote(term, this.name);
                } catch (WebServiceException wse) {
                    System.err.println("Web service no longer exists, must be dead");
                }
            }
        }

        //check for majority
        System.out.println(name + ": recieved " + numVotes + " votes. For majority need more than " + (servers.size() / 2));
        if (numVotes > servers.size() / 2) {
            System.out.println("Term: " + term + "\t" + name + ": has majority");

            //Winner winner chicken dinner
            state = State.LEADER;
            System.out.println("Term: " + term + "\t" + name + ": is now leader for new term " + term);


            //need to grab an instance of jguddi and tell it i'm the leader
            Registry registry;
            try {
                registry = LocateRegistry.getRegistry();
                IJguddiService jguddi = null;
                for (String service : registry.list()) {
                    jguddi = (IJguddiService) registry.lookup(service);
                }
                jguddi.setLeaderEndpoint(myEndpoint);
                System.out.println(name + " :told jguddi that I am the new leader");
            } catch (RemoteException re) {
                System.err.println(name + " can't tell registry i'm the new leader (remote exception), will step down");
                return;  // dont start heartbeat new election will happen
            } catch (NotBoundException nbe) {
                System.err.println(name + " can't tell registry i'm the new leader (not bound exception), will step down");
                return;  // dont start heartbeat new election will happen
            }

            //I am now the leader!!! must start heat beat or my followers will try and take over :(
            heartBeat = new Thread(new HeartBeat(this));
            heartBeat.start();
        } else {
            //something went wrong, back into the running,
            state = State.FOLLOWER;
            resetElectionTimer();
        }

    }

    @Override
    public int requestVote(int term, String name) {
        //if I havent voted in the requesting serverEndpoints term return a vote


        // If I'm voting for myself
        if (name.equals(this.name)) {
            System.out.println("Term: " + term + "\t" + this.name + ": voted for themselves");
            return 1;
        } else if (state == State.FOLLOWER) {    // If I'm not voting for itself and i'm a follower
            if (this.term < term) {
                System.out.println(name + " is asking " + this.name + " to vote for them");
                System.out.println("Term: " + term + "\t" + this.name + ": voted for " + name);
                //catch up term to the current
                this.term = term;
                resetElectionTimer();
                return 1;
            }
        }


        return 0;
}

    public void beatHeart() {

        System.out.println("Term: " + term + "\t" + name + ": badump");

        //need to tell the other serverEndpoints that we're alive
        int numberServersWhoRecieved = 0;
        List<IRaftServer> serversWhoRecieved = new Vector<>();
        synchronized (servers) {
            for (IRaftServer s : servers) {
                try {
                    //the current log is appended and sent around
                    String whoRecieved = null;
                    whoRecieved = s.recieveHeartBeat(coordinator.getLog() + latestVotes, name);
                    numberServersWhoRecieved++;
                    if (!whoRecieved.equals(name)) {
                        System.out.println("Term: " + term + "\t" + name + ": knows that " + whoRecieved + " recieved the heart beat");
                    }
                } catch (WebServiceException wse) {
                    System.err.println("Web service no longer exists, must be dead");

                }

            }
            //after telling every other server what to do, reset latest votes to null
            latestVotes = "";

            // upon acknowledgement of reciept from majority, update my own data base, and send notification to all to update their database too
            if (numberServersWhoRecieved > servers.size() / 2) {
                //tell other servers to commit an make sure it's synchronised too!
                for (IRaftServer server : servers) {
                    try {
                        server.commitStagingArea();
                    } catch (ClientTransportException e) {
                        System.err.println("A server has died");
                    } catch (WebServiceException e2) {
                        System.err.println("A server has died");
                    }

                }
            }
        }

    }

    public String recieveHeartBeat(String data, String sentBy) {
        if (!sentBy.equals(name)) {

            System.out.println("Term: " + term + "\t" + name + ": recieved the heart beat" + " from " + sentBy);
            resetElectionTimer();
        }
        //stage the data for committing
        if (!data.equals("")) System.out.println(name + "adding this data to staging area: " + data);
        if (!data.equals("")) coordinator.addToStagingArea(data);
        return name;


    }

    //if a majority recieved the heart beat, then the data that came with that will be commited to db
    public void commitStagingArea() {
        coordinator.commitStagingArea();
    }

    private void resetElectionTimer() {
        electionTimer.interrupt();
        electionTimer = new Thread(new ElectionTimer(this));
        electionTimer.start();
    }


private class ElectionTimer implements Runnable {

    private IElectionTimerCallBack context;

    public ElectionTimer(IElectionTimerCallBack context) {
        this.context = context;
    }

    @Override
    public void run() {
        try {
            Random rand = new Random();
            int time = rand.nextInt(RAND_ELEC_TIME) + RAND_ELEC_TIME;
            Thread.sleep((long) time);
            context.electionTimerUp();
        } catch (InterruptedException e) {
            // no need to handle, I think that if the thread gets interupted,
            // then a heart beat has arrived, all is well
        }
    }
}

private class HeartBeat implements Runnable {


    IHeartBeatCallBack context;

    public HeartBeat(IHeartBeatCallBack context) {
        this.context = context;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Thread.sleep(PULSE);
                context.beatHeart();
            }
        } catch (InterruptedException e) {
            //TODO do i need to handle?
        }
    }
}

private class OtherServerChecker implements Runnable {

    IOtherServerCheckerCallBack context;

    public OtherServerChecker(IOtherServerCheckerCallBack context) {
        this.context = context;
    }

    @Override
    public void run() {
        try {
            while (true) {
                context.getServersFromJguddi();
                Thread.sleep(OTHER_SERVER_CHECK_PAUSE);
            }
        } catch (InterruptedException e) {
            //TODO do i need to handle?
        }
    }

}

    @Override
    public void castVote(String name) {
        latestVotes += (name + ",");
    }

    @Override
    public String getCurrentVotes() {
        return coordinator.getCurrentResults();
    }

}



