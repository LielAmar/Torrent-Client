# Who did what
<b><u>Liel:</b></u> 
Worked on the program main entry point, the design and handle of connections,
bitfield, handshake and piece packets, opening the file and dumping the file after it's received, and closing the connections.
Worked on the Logger, ConnectionHandler and updating unchoked neighbors and optimistic neighbor with eyal.

<b><u>Eyal:</b></u>
Worked on parsing and replying to all packets, worked on handling piece information, preserving connection state and information on all peers,
and on receiving and handling incoming packets. 
Worked on the Logger, ConnectionHandler and updating unchoked neighbors and optimistic neighbor with liel.

<b><u>Matthew:</b></u>
Worked on the listener and sender design and initial implementation, worked on the connection to UF remote machines,
Worked on the Internal Messages and the connection manager. Improved the ConnectionHandler and worked on preventing race conditions.
Worked on the initial OOP design of packets and on the LocalPeerManager implementation.

# Compiling the code
```javac -d . src/project/*.java src/project/connection/*.java src/project/connection/piece/*.java src/project/exceptions/*.java src/project/message/*.java src/project/message/packet/*.java src/project/message/packet/packets/*.java src/project/message/InternalMessage/*.java src/project/message/InternalMessage/InternalMessages/*.java src/project/utils/*.java ```

# Running the code
```java project.PeerProcess```