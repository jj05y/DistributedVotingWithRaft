# DistributedVotingWithRaft

- Clients will communicate with server
- The leader server must publish itself so the client can use it

- There will be just one vote endpoint published
  - voting done via an IVotingService with one method castVote(String/Int candidate);

- Each server will have to publish itself for raft too to allow inter server communication
- server.server "chat" needs to be defined, ie, what communication is needed between servers
  - assigning the 'leader'
  - heartbeat
  - consensus


## Each server will have:
- A data base with controller (wrapper)
- A Jax-Ws implementation that will:
  - Communicate with other servers
  - Potentially recieve votes (if leader)
  - Communicate transactions with it's own controller.
- The controller will be responsible for maintaining the transaction log, aswell as carrying out transactions

## Each client
- will just grab an instace of IVotingService from the published endpoint and cast votes :)