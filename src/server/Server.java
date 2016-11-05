package server;

import registry.Registry;

import java.util.Random;

/**
 * Created by joe on 05/11/16.
 */
public class Server implements IElectionTimerCallBack, IHeartBeatCallBack {


    private static int PULSE = 250;
    private Server leader;

    private enum State{CANDIDATE, LEADER, FOLLOWER};
    private State state;
    private Thread electionTimer;
    private Thread heartBeat;
    private int term;
    private String name;

    public Server(String name) {
        this.name = name;
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
        for (Server s : Registry.SERVERS.values()) {
            numVotes += s.requestVote(term);
        }

        //check for majority
        System.out.println(name + ": recieved " + numVotes + " votes");
        if (numVotes > Registry.SERVERS.size()/2) {
            System.out.println(name + ": has majority");

            //Winner winner chicken dinner
            state = State.LEADER;
            System.out.println(name + ": is now leader for new term " + term);
            //notify the other servers
            for (Server s : Registry.SERVERS.values()) {
                if (s != this) s.defineLeader(this);
            }
            //I am now the leader!!! must start heat beat or my followers will try and take over :(
            heartBeat = new Thread(new HeartBeat(this));
            heartBeat.start();
        }




    }

    public int requestVote(int term) {
        //if I havent voted in the requesting servers term return a vote
        if (this.term < term) {
            System.out.println(name + ": voted in term " + this.term);
            //increment term to stop any more voting requests
            this.term++;
            return 1;
        }
        return 0;
    }

    public void defineLeader(Server s) {
        leader = s;
        resetElectionTimer();
        state = State.FOLLOWER;
    }

    public void beatHeart() {

        System.out.println(name + ": badump");

        //need to tell the other servers that we're alive
        for (Server s : Registry.SERVERS.values()) {
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



