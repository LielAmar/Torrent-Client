package project;

import project.connection.piece.PieceStatus;
import project.utils.Triplet;

import java.io.*;

import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import java.nio.file.Paths;

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
        } catch (IOException exception) {
            System.out.println("An error occurred when trying to set up the configuration!");
            System.exit(1);
        }

        System.out.println("[CONFIGURATION] Finished setting up the configuration\n");
    }

    /**
     * Parses the peerInfo.cfg file, opens connections with previous peers & starts listening to incoming connections.
     */
    private static void setupPeerConnections() {
        System.out.println("[CONNECTIONS] Setting up peer connections");

        AtomicInteger localPort = new AtomicInteger();
        List<Triplet<Integer, String, Integer>> pendingConnections = new ArrayList<>();

        try (Stream<String> stream = Files.lines(Paths.get(PEER_INFO_CONFIG_FILE))) {
            stream.forEach(line -> {
                String[] values = line.split(" ");

                int peerId = Integer.parseInt(values[0]);

                if(peerId < localPeerId) {
                    String hostname = values[1];
                    int port = Integer.parseInt(values[2]);

                    pendingConnections.add(new Triplet<>(peerId, hostname, port));
                } else if(peerId == localPeerId) {
                    boolean hasFile = Integer.parseInt(values[3]) == 1;

                    loadLocalPieces(hasFile);

                    localPort.set(Integer.parseInt(values[2]));
                }
            });
        } catch (IOException exception) {
            System.out.println("An error occurred when trying to set up connections!");
            System.exit(1);
        }

        // Connect to all previous peers
        for(Triplet<Integer, String, Integer> triplet : pendingConnections) {
            connectToPeer(triplet.getFirst(), triplet.getSecond(), triplet.getThird());
        }

        // Start listening to incoming connections
        listenToIncomingConnections(localPort.get());
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
        } catch (IOException exception) {
            System.out.println("An error occurred when trying to connect to a remote peer!");
            System.exit(1);
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
        } catch (IOException exception) {
            System.out.println("An error occurred when a remote peer tried to connect to server!");
            System.exit(1);
        }
    }

    /**
     * Loads the local pieces into the ram
     *
     * @param hasFile   Whether the local peer has the file
     */
    private static void loadLocalPieces(boolean hasFile) {
        if(hasFile) {
            String filePath = "RunDir" + File.separator + "peer_" + localPeerId + File.separator + PeerProcess.config.getFileName();
            File file = new File(filePath);

            byte[] data = new byte[(int) file.length()];

            try(FileInputStream fis = new FileInputStream(file)) {
                fis.read(data);
            } catch (IOException exception) {
                System.out.println("An error occurred when tried to read file to transfer!");
                System.exit(1);
            }

            for(int i = 0; i < PeerProcess.config.getNumberOfPieces(); i++) {
                int pieceSize = Math.min(PeerProcess.config.getPieceSize(), PeerProcess.config.getFileSize() - i * PeerProcess.config.getPieceSize());
                byte[] buffer = new byte[pieceSize];
                System.arraycopy(data, i * PeerProcess.config.getPieceSize(), buffer, 0, buffer.length);
                PeerProcess.localPeerManager.setLocalPiece(i, PieceStatus.HAVE, buffer);
            }
        } else {
            // Set all local pieces to NOT_HAVE with null content
            for(int i = 0; i < PeerProcess.config.getNumberOfPieces(); i++) {
                PeerProcess.localPeerManager.setLocalPiece(i, PieceStatus.NOT_HAVE, null);
            }
        }
    }
}