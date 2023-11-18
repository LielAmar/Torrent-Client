package project;

import project.connection.piece.PieceStatus;
import project.utils.Triplet;

import java.io.*;

import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class PeerProcess {

    private final static String COMMON_CONFIG_FILE = "Common.cfg";
    private final static String PEER_INFO_CONFIG_FILE = "PeerInfo.cfg";

    public static Configuration config; // TODO: might wanna change config to not be static somehow...
    public static LocalPeerManager localPeerManager;

    private static int localPeerId;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Please specify a Peer ID!");
            return;
        }

        // Set up the configuration
        setupConfiguration();

        // Set up the local peer manager
        localPeerId = Integer.parseInt(args[0]);
        PeerProcess.localPeerManager = new LocalPeerManager(localPeerId, PeerProcess.config.getNumberOfPieces());

        // Set up all peer connections
        setupPeerConnections();
    }


    /**
     * Parses the Common.cfg file and sets up the configuration
     * This function reads Common.cfg line by line, and then passes each line's data into the
     * #Configuration.constructor function, which creates a configuration object
     */
    private static void setupConfiguration() {
        System.out.println("[CONFIGURATION] Setting up the configuration");

        try (Stream<String> stream = Files.lines(Paths.get(COMMON_CONFIG_FILE))) {
            String[] rows = stream.map(line -> line.replaceAll("\r", "")).toArray(String[]::new);

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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("[CONFIGURATION] Finished setting up the configuration\n");
    }

    /**
     * Parses the peerInfo.cfg file, starts listening to incoming connections and opens connections with all
     * previous peers
     * TODO: reformat this function
     */
    private static void setupPeerConnections() {
        System.out.println("[CONNECTIONS] Setting up peer connections");

        AtomicInteger port = new AtomicInteger();
        List<Triplet<Integer, String, Integer>> pendingConnections = new ArrayList<>();

        try (Stream<String> stream = Files.lines(Paths.get(PEER_INFO_CONFIG_FILE))) {
            stream.forEach(line -> {
                String[] values = line.split(" ");

                int peerId = Integer.parseInt(values[0]);
                port.set(Integer.parseInt(values[2]));

                if(peerId < localPeerId) {
                    String hostname = values[1];

                    pendingConnections.add(new Triplet<>(peerId, hostname, port.get()));
                } else {
                    boolean hasFile = Integer.parseInt(values[3]) == 1;

                    loadLocalPieces(hasFile);
                }
            });
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        // Connect to all previous peers
        for(Triplet<Integer, String, Integer> triplet : pendingConnections) {
            connectToPeer(triplet.getFirst(), triplet.getSecond(), triplet.getThird());
        }

        // Start listening to incoming connections
        listenToIncomingConnections(port.get());
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
            // Create a socket connection to the given peer and then create two peer connections:
            // 1. A listener connection for listening to incoming messages
            // 2. A sender connection for sending outgoing messages
            Socket socket = new Socket(hostname, port);

            System.out.println("[CLIENT] Attempting to connect to peer " + peerId + " at host " + hostname + ":" + port);

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
                    // Create a socket connection to the given peer and then create two peer connections:
                    // 1. A listener connection for listening to incoming messages
                    // 2. A sender connection for sending outgoing messages
                    Socket socket = listener.accept();

                    System.out.println("[SERVER] Attempting to connect to peer " + "-1" + " at host " + "unknown" + ":" + "unknown");

                    PeerProcess.localPeerManager.connectToNewPeer(socket).start();
                }
            }
        } catch (IOException e) {
            // TODO: Close socket
            throw new RuntimeException(e);
        }
    }


    private static void loadLocalPieces(boolean hasFile) {
        if(hasFile) {
            try {
                // Read the local file's bytes, and set all pieces to HAVE with their content
                byte[] data = Files.readAllBytes(new File("RunDir/peer_" + localPeerId + File.separator + PeerProcess.config.getFileName()).toPath());

                // Set all local pieces to HAVE and set their content
                for(int i = 0; i < PeerProcess.config.getNumberOfPieces(); i++) {
                    int pieceSize = Math.min(PeerProcess.config.getPieceSize(), PeerProcess.config.getFileSize() - i * PeerProcess.config.getPieceSize());
                    byte[] buffer = new byte[pieceSize];
                    System.arraycopy(data, i * PeerProcess.config.getPieceSize(), buffer, 0, buffer.length);
                    PeerProcess.localPeerManager.setLocalPiece(i, PieceStatus.HAVE, buffer);
                }
            } catch(IOException exception) {
                exception.printStackTrace();
            }
        } else {
            // Set all local pieces to NOT_HAVE with null content
            for(int i = 0; i < PeerProcess.config.getNumberOfPieces(); i++) {
                PeerProcess.localPeerManager.setLocalPiece(i, PieceStatus.NOT_HAVE, null);
            }
        }
    }
}
