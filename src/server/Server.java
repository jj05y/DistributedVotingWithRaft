package server;

import jguddi.Endpoint;
import jguddi.IJguddiService;
import jguddi.JguddiService;
import registry.Registry;

import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
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

@WebService(name="IServer")
public class Server implements IElectionTimerCallBack, IHeartBeatCallBack, IServer {
    private List<Endpoint> serverEndpoints;
    private List<IServer> servers;
    private static int PULSE = 250;

    private enum State{CANDIDATE, LEADER, FOLLOWER};
    private State state;
    private Thread electionTimer;
    private Thread heartBeat;
    private int term;
    private String name;

    public Server() {}

    public Server(String name) {
        this.name = name;
        serverEndpoints = new Vector<>();
        servers = new Vector<>();
        getListFromJguddi();
        resolveOtherServers();
        startServer();
    }

    private void resolveOtherServers() {
        try {
            for (Endpoint ep : serverEndpoints) {
                Service service = Service.create(new URL(ep.getUrl()), new QName(ep.getqName(), ep.getQname2()));
                servers.add(service.getPort(IServer.class));
            }
        } catch(MalformedURLException mue) {
            System.out.println("Bad Url::");
        }
    }

    private void getListFromJguddi() {
        java.rmi.registry.Registry registry = null;
        try {
            registry = LocateRegistry.getRegistry();
        } catch (RemoteException e) {
            System.out.println("WARNING: Failed to get or create RMI Registry");
        }
        try {
            IJguddiService jguddi = null;
            for (String service : registry.list()) {
                jguddi = (IJguddiService) registry.lookup(service);
            }
            if (jguddi != null) serverEndpoints = jguddi.getEndpoints();
        } catch(RemoteException re) {

        } catch (NotBoundException ne) {

        }
    }


    public void startServer() {
        state = State.FOLLOWER;
        electionTimer = new Thread(new ElectionTimer(this));
        electionTimer.start();
    }



    @Override
    public void electionTimerUp() {
        System.out.println(name + ": Election timer up");
        //request votes

        state = State.CANDIDATE;
        System.out.println(name + ": is now candidate");
        //new term!
        term++;
        int numVotes = 0;
        for (IServer s : servers) {
            numVotes += s.requestVote(term);
        }

        //check for majority
        System.out.println(name + ": recieved " + numVotes + " votes");
        if (numVotes > servers.size()/2) {
            System.out.println(name + ": has majority");

            //Winner winner chicken dinner
            state = State.LEADER;
            System.out.println(name + ": is now leader for new term " + term);

            //I am now the leader!!! must start heat beat or my followers will try and take over :(
            heartBeat = new Thread(new HeartBeat(this));
            heartBeat.start();
        } else {
            //something went wrong, back into the running,
            state = State.FOLLOWER;
            resetElectionTimer();
        }

    }

    public int requestVote(int term) {
        //if I havent voted in the requesting serverEndpoints term return a vote
        if (this.term < term) {
            System.out.println(name + ": voted in term " + this.term);
            //increment term to stop any more voting requests
            this.term++;
            resetElectionTimer();
            return 1;
        }
        return 0;
    }

    public void beatHeart() {

        System.out.println(name + ": badump");

        //need to tell the other serverEndpoints that we're alive
        for (IServer s : servers) {
            if (s != this) {
                //TODO send data with heart beat
                String whoRecieved = s.recieveHeartBeat(null);
                System.out.println(name + ": knows that " + whoRecieved + " recieved the heart beat");
            }
            //TODO upon ackknowledgement of reciept from majority, update my own data base, and send notification to all to update their database too
        }

    }

    public String recieveHeartBeat(Object data) {
        //TODO stage data for commit to DB
        System.out.println(name + ": recieved the heart beat");
        resetElectionTimer();
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
                int time = rand.nextInt(1000) + 1000;
                Thread.sleep((long)time);
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
                while (i++ < 3) {
                    Thread.sleep(PULSE);
                    context.beatHeart();
                }
            } catch (InterruptedException e) {
                //TODO do i need to handle?
            }
        }
    }
}



