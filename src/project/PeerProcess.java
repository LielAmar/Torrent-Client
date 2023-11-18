package project;

import project.connection.piece.PieceStatus;

import java.io.*;

import java.net.ServerSocket;
import java.net.Socket;

public class PeerProcess {

    private final static String COMMON_CONFIG_FILE = "Common.cfg";
    private final static String PEER_INFO_CONFIG_FILE = "PeerInfo.cfg";

    public static Configuration config; // TODO: might wanna change config to not be static somehow...
    public static LocalPeerManager localPeerManager;


    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Please specify a Peer ID!");
            return;
        }

        // Set up the configuration
        setupConfiguration();

        // Set up the local peer manager
        int localPeerId = Integer.parseInt(args[0]);
        PeerProcess.localPeerManager = new LocalPeerManager(localPeerId, PeerProcess.config.getNumberOfPieces());

        // Set up all peer connections
        setupPeerConnections(localPeerId);
    }


    /**
     * Parses the Common.cfg file and sets up the configuration
     */
    private static void setupConfiguration() {
        System.out.println("[CONFIGURATION] Setting up the configuration");
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

            System.out.println(PeerProcess.config);
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        System.out.println("[CONFIGURATION] Finished setting up the configuration\n");
    }

    /**
     * Parses the peerInfo.cfg file, opens connections with previous peers & starts listening to incoming connections.
     *
     * @param localPeerId   The ID of the local peer process
     */
    private static void setupPeerConnections(int localPeerId) {
        System.out.println("[CONNECTIONS] Setting up peer connections");

        try (BufferedReader br = new BufferedReader(new FileReader((new File(PEER_INFO_CONFIG_FILE)).getAbsolutePath()))) {
            String line;

            // Go over all peers preceding current peer and create a connection with them
            // Once reaching current peer, open a socket server to listen to future connections with future peers
            while ((line = br.readLine()) != null) {
                String[] values = line.split(" ");

                int peerId = Integer.parseInt(values[0]);
                int port = Integer.parseInt(values[2]);

                // Peers previous to local peer
                if (peerId < localPeerId) {
                    String hostname = values[1];

                    connectToPeer(peerId, hostname, port);
                // Local peer
                } else {
                    // If the current peer has the file, load the file into its local piece list
                    if(Integer.parseInt(values[3]) == 1) {
                        String filePath = "peer_" + peerId + File.separator + PeerProcess.config.getFileName();

                        try (RandomAccessFile file = new RandomAccessFile(filePath, "r")) {
                            byte[] buffer = new byte[PeerProcess.config.getPieceSize()];

                            for(int i = 0; i < PeerProcess.config.getNumberOfPieces(); i++) {
                                int bytesRead = file.read(buffer);

                                PeerProcess.localPeerManager.setLocalPiece(i, PieceStatus.HAVE, buffer);
                            }
                        } catch (FileNotFoundException exception) {
                            exception.printStackTrace();
                        }
                    }

                    // Start listening to incoming connections
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
            System.out.println("[CLIENT] Attempting to connect to peer " + peerId + " at host " + hostname + ":" + port);

            // Create a socket connection to the given peer and then create two peer connections:
            // 1. A listener connection for listening to incoming messages
            // 2. A sender connection for sending outgoing messages
            Socket socket = new Socket(hostname, port);
            PeerProcess.localPeerManager.connectToNewPeer(peerId, socket).start();
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
                    System.out.println("[SERVER] Attempting to connect to peer " + "-1" + " at host " + "unknown" + ":" + "unknown");

                    // Create a socket connection to the given peer and then create two peer connections:
                    // 1. A listener connection for listening to incoming messages
                    // 2. A sender connection for sending outgoing messages
                    Socket socket = listener.accept();
                    PeerProcess.localPeerManager.connectToNewPeer(socket).start();
                }
            }
        } catch (IOException e) {
            // TODO: Close socket
            throw new RuntimeException(e);
        }
    }
}
