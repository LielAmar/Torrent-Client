package project;

import project.connection.ConnectionState;
import project.connection.PeerConnectionManager;
import project.peer.Peer;
import project.peer.PeerInfoList;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class PeerProcess {

    private final static String COMMON_CONFIG_FILE = "Common.cfg";
    private final static String PEER_INFO_CONFIG_FILE = "PeerInfo.cfg";

    public static Configuration config;
    public static PeerInfoList peerInfoList;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Please specify a peerId!");
            return;
        }

        setupConfiguration();

        int localPeerId = Integer.parseInt(args[0]);
        PeerProcess.peerInfoList = new PeerInfoList(localPeerId);

        setupPeerConnections(localPeerId);
    }


    /**
     * Parses the Common.cfg file and sets up the configuration
     */
    private static void setupConfiguration() {
        try (BufferedReader br = new BufferedReader(new FileReader((new File(COMMON_CONFIG_FILE)).getAbsolutePath()))) {
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
            }

            // remove all \r from the string, as this breaks the integer conversion
            while(sb.indexOf("\r") != -1) {
                sb.deleteCharAt(sb.indexOf("\r"));
            }

            // split the converted string by row
            String[] rows = sb.toString().split("\n");

            // Set up the configuration object
            PeerProcess.config = new Configuration(
                    Integer.parseInt(rows[0].split(" ")[1]), // number of preferred neighbors
                    Integer.parseInt(rows[1].split(" ")[1]), // unchoking interval
                    Integer.parseInt(rows[2].split(" ")[1]), // optimistic unchoking interval
                    rows[3].split(" ")[1],                   // file name
                    Integer.parseInt(rows[4].split(" ")[1]), // file size
                    Integer.parseInt(rows[5].split(" ")[1])  // piece size
            );

            System.out.println("[CONFIG] Created process configuration!");
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Parses the peerInfo.cfg file, opens connections with previous peers & starts listening to incoming connections.
     *
     * @param localPeerId   The ID of the local peer process
     */
    private static void setupPeerConnections(int localPeerId) {
        try (BufferedReader br = new BufferedReader(new FileReader((new File(PEER_INFO_CONFIG_FILE)).getAbsolutePath()))) {
            String line;

            // Go over all peers preceding current peer and create a connection with them
            // Once reaching current peer, open a socket server to listen to future connections with future peers
            while ((line = br.readLine()) != null) {
                String[] values = line.split(" ");

                int peerId = Integer.parseInt(values[0]);
                int port = Integer.parseInt(values[2]);

                if (peerId < localPeerId) {
                    String hostname = values[1];

                    connectToPeer(peerId, hostname, port);
                } else {
                    // TODO: maybe move this outside of the while loop? it doesnt feel right to have this loop sitting unfinished when we call another function that just starts and infinite loop
                    // If the current peer has the file, make sure to put it in the config
                    if(Integer.parseInt(values[3]) == 1) {
                        for(int i = 0; i < PeerProcess.config.getLocalPieces().length; i++) {
                            PeerProcess.config.setLocalPiece(i, PieceStatus.HAVE);
                        }
                    }

                    listenToIncomingConnections(port);
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Connects to a different peer process that's listening to connections
     *
     * @param peerId     ID of the peer to connect to
     * @param hostname   Hostname of the peer to connect to
     * @param port       Port of the peer to connect to
     */
    private static void connectToPeer(int peerId, String hostname, int port) {
        try {
            System.out.println("[CLIENT] Creating connection to peerId: " + peerId + " at host " + hostname + ":" + port);

            // Create a socket connection to the given peer and then create two peer connections:
            // 1. A listener connection for listening to incoming messages
            // 2. A sender connection for sending outgoing messages
            Socket socket = new Socket(hostname, port);

            ConnectionState state = PeerProcess.peerInfoList.connectToNewPeer(peerId);
            PeerConnectionManager connectionManager = new PeerConnectionManager(socket, state);
            connectionManager.start();  
        } catch (IOException e) {
            // TODO: Close sockets
            throw new RuntimeException(e);
        }
    }

    /**
     * Listens to incoming peer connection requests
     *
     * @param port   Port on which to listen
     */
    private static void listenToIncomingConnections(int port) {
        try {
            try (ServerSocket listener = new ServerSocket(port)) {
                System.out.println("[SERVER] Listening to connections from peers on port " + port);

                while (true) {
                    // Create a socket connection to the given peer and then create two peer connections:
                    // 1. A listener connection for listening to incoming messages
                    // 2. A sender connection for sending outgoing messages
                    Socket socket = listener.accept();
                    ConnectionState state = peerInfoList.connectToNewPeer();

                    PeerConnectionManager connectionManager = new PeerConnectionManager(socket, state);
                    connectionManager.start();  

                    System.out.println("[SERVER] Received a connection request from another peer!");
                }
            }
        } catch (IOException e) {
            // TODO: Close socket
            throw new RuntimeException(e);
        }
    }
}
