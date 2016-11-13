package server;

import jguddi.IJguddiService;

import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.List;
import java.util.Random;
import java.util.Vector;

/**
 * Created by joe on 05/11/16.
 */

@WebService(name = "IServer")
public class Server implements IElectionTimerCallBack, IHeartBeatCallBack, IServer, IOtherServerCheckerCallBack {
    public static final int OTHER_SERVER_CHECK_PAUSE = 1000;
    private List<IServer> servers;      //Other servers on Jguddi registry
    public static final int RAND_ELEC_TIME = 1500;
    private static int PULSE = 500;

    private enum State {CANDIDATE, LEADER, FOLLOWER}

    private State state;
    private Thread electionTimer;
    private Thread heartBeat;
    private int term;
    private String name;

    public Server() {
    }

    public Server(String name) {
        this.name = name;
        servers = new Vector<>();
        getServersFromJguddi();
        (new Thread(new OtherServerChecker(this))).start();
        startServer();
    }

    public void getServersFromJguddi() {
        List<String> serverEndpoints = new Vector<>();

        java.rmi.registry.Registry registry = null;
        try {
            registry = LocateRegistry.getRegistry();
        } catch (RemoteException e) {
            System.out.println("WARNING: Failed to get or create RMI Registry");
        }
        try {   //TODO: when there's only one server this will constantly spew remoteExceptions, handle?
            IJguddiService jguddi = null;
            for (String service : registry.list()) {
                jguddi = (IJguddiService) registry.lookup(service);
            }
            if (jguddi != null) serverEndpoints = jguddi.getEndpoints();
        } catch (RemoteException re) {
            System.out.println("remote exception");
        } catch (NotBoundException ne) {
            System.out.println("Service Not Bound, server is dead");
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
                    servers.add(service.getPort(IServer.class));
                } catch (WebServiceException wse) {
                    System.out.println("Web service no longer exists, must be dead");
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
                System.out.println("remote exception");
            } catch (NotBoundException ne) {
                System.out.println("Service Not Bound, server is dead");
            }

        } catch (MalformedURLException mue) {
            System.out.println("Bad Url::");
        }
        System.out.println("There are now " + servers.size() + " sever(s)");
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
            for (IServer s : servers) {
                try {
                    numVotes += s.requestVote(term, this.name);
                } catch (WebServiceException wse) {
                    System.out.println("Web service no longer exists, must be dead");
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
        if (name.equals(this.name)){
            System.out.println("Term: " + term + "\t" + this.name + ": voted for themselves");
            return 1;
        } else {    // If I'm not voting for itself
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
        synchronized (servers) {
            for (IServer s : servers) {
                try {
                    //TODO send data with heart beat
                    String whoRecieved = null;
                    whoRecieved = s.recieveHeartBeat(null, name);
                    if (!whoRecieved.equals(name)) {
                        System.out.println("Term: " + term + "\t" + name + ": knows that " + whoRecieved + " recieved the heart beat");
                    }
                } catch (WebServiceException wse) {
                    System.out.println("Web service no longer exists, must be dead");

                }

            }
            //TODO upon acknowledgement of reciept from majority, update my own data base, and send notification to all to update their database too
        }

    }

    public String recieveHeartBeat(Object data, String sentBy) {
        if (!sentBy.equals(name)) {
            //TODO stage data for commit to DB
            System.out.println("Term: " + term + "\t" + name + ": recieved the heart beat" + " from " + sentBy);
            resetElectionTimer();
        }
        return name;


    }

    //TODO write method for commit Data to DB

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
                //TODO while true cos a heart should beat for ever
                int i = 0;
                while (i++ < 30) {
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
}



