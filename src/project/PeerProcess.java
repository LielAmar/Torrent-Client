package project;

import project.utils.Logger;
import project.utils.Tag;

import java.io.*;

import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import java.nio.file.Paths;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class PeerProcess {

    private final static String COMMON_CONFIG_FILE = "Common.cfg";
    private final static String PEER_INFO_CONFIG_FILE = "PeerInfo.cfg";

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("No Peer ID was specified");
            System.exit(1);
        }

        // Set up the configuration
        Configuration config = setupConfiguration(COMMON_CONFIG_FILE);

        // Set up the local peer manager
        int localPeerId = Integer.parseInt(args[0]);
        LocalPeerManager localPeerManager = new LocalPeerManager(localPeerId, config);

        // Set up all peer connections
        setupPeerConnections(PEER_INFO_CONFIG_FILE, localPeerManager);
    }


    private static class Peer {

        /*
        A local class used to save data on peers from the peer info file
         */

        final int peerId;
        final String hostname;
        final int port;
        final boolean hasFile;

        Peer(String line) {
            peerId = Integer.parseInt(line.split(" ")[0]);
            hostname = line.split(" ")[1];
            port = Integer.parseInt(line.split(" ")[2]);
            hasFile = Integer.parseInt(line.split(" ")[3]) == 1;
        }
    }


    /**
     * Opens the given configuration file and sets up a configuration object to be used throughout the program
     * In case the configuration is unable to be set up properly, a system exit would execute with code 1.
     *
     * @param configurationPath   Path to the configuration file to parse
     * @return                    Configuration object
     */
    private static Configuration setupConfiguration(String configurationPath) {
        Logger.print(Tag.CONFIGURATION, "Setting up the configuration");

        Configuration config = null;

        // Attempts to open the configuration file given, and create the configuration object
        try (Stream<String> stream = Files.lines(Paths.get(configurationPath))) {
            String[] rows = stream.map(line -> line.replaceAll("\r", "")).toArray(String[]::new);

            config = new Configuration(
                    Integer.parseInt(rows[0].split(" ")[1]), // number of preferred neighbors
                    Integer.parseInt(rows[1].split(" ")[1]), // unchoking interval
                    Integer.parseInt(rows[2].split(" ")[1]), // optimistic unchoking interval
                    rows[3].split(" ")[1],                   // file name
                    Integer.parseInt(rows[4].split(" ")[1]), // file size
                    Integer.parseInt(rows[5].split(" ")[1])  // piece size
            );

            System.out.println(config);
        } catch (NumberFormatException exception) {
            System.err.println("An error occurred when trying to parse some values in the given configuration file!");
            System.exit(1);
        } catch (IOException exception) {
            System.err.println("An error occurred when trying to open the given configuration file!");
            System.exit(1);
        }

        Logger.print(Tag.CONFIGURATION, "Finished setting up the configuration");

        return config;
    }

    /**
     * Opens the given peer info file and sets up all connections to all other peers.
     * In case the connections are unable to be set up properly, a system exit would execute with code 1.
     *
     * @param peerInfoPath       Path to the peer info file to parse
     * @param localPeerManager   An object representing the local peer
     */
    private static void setupPeerConnections(String peerInfoPath, LocalPeerManager localPeerManager) {
        Logger.print(Tag.CONNECTIONS, "Setting up peer connections");

        List<Peer> pendingConnections = new ArrayList<>();
        AtomicReference<Peer> localPeer = new AtomicReference<>(null);
        AtomicInteger expectedConnections = new AtomicInteger(0);

        // Attempts to open the peer info file given, and parse each line, which represents a single peer
        try (Stream<String> stream = Files.lines(Paths.get(peerInfoPath))) {
            stream.forEach(line -> {
                Peer peer = new Peer(line);

                // If the current parsed peer the local peer, set it up
                // Otherwise, set up the remote peer for either pending or expected connection
                if(peer.peerId == localPeerManager.getLocalPeerId()) {
                    localPeer.set(peer);
                } else {
                    // If the local peer has not yet been set, it means this is a pending connection, otherwise it's
                    // an expected connection
                    if(localPeer.get() == null) {
                        pendingConnections.add(peer);
                    } else {
                        expectedConnections.incrementAndGet();
                    }
                }
            });
        } catch (NumberFormatException exception) {
            System.err.println("An error occurred when trying to parse some values in the given peer info file!");
            System.exit(1);
        } catch (IOException exception) {
            System.err.println("An error occurred when trying to open the given peer info file!");
            System.exit(1);
        }

        if(localPeer.get() == null) {
            System.err.println("The given peer id didn't match any peer in the peer info file!");
            System.exit(1);
        }

        // Finish setting up the local peer's pieces according to whether it has the file or not
        try {
            localPeerManager.setupInitialPieces(localPeer.get().hasFile);
        } catch (IOException exception) {
            System.err.println("An error occurred when trying to open the local target file!");
            System.exit(1);
        }

        // Open connections to all peers previous to local peer
        for(Peer peer : pendingConnections) {
            PeerProcess.connectToPeer(localPeerManager, peer);
        }

        // Listen to connections from all expected peers
        PeerProcess.listenToIncomingConnections(localPeerManager, localPeer.get().port, expectedConnections.get());
    }


    /**
     * Connects the local peer (client) to a remote peer (server) by:
     * 1. Creating a socket with the remote peer's hostname and port
     * 2. Calling PeerProcess#connectToPeer
     *
     * @param localPeerManager   An object representing the local peer
     * @param peer               Peer to connect to
     */
    private static void connectToPeer(LocalPeerManager localPeerManager, Peer peer) {
        try {
            Logger.print(Tag.CLIENT, "Attempting to connect to peer " +
                    peer.peerId + " at host " + peer.hostname + ":" + peer.port);

            Socket socket = new Socket(peer.hostname, peer.port);
            localPeerManager.connectToPeer(peer.peerId, socket).start();
        } catch (IOException exception) {
            System.err.println("An error occurred when trying to connect to a remote peer");
            System.exit(1);
        }
    }

    /**
     * Connects the local peer (server) to a remote peer (client) by:
     * 1. Accepting a socket with the remote peer
     * 2. Calling PeerProcess#connectToPeer
     *
     * @param localPeerManager      An object representing the local peer
     * @param localPort             Port on which to listen to incoming peer connections
     * @param expectedConnections   Number of expected connections to have
     */
    private static void listenToIncomingConnections(LocalPeerManager localPeerManager, int localPort,
                                                    int expectedConnections) {
        try {
            try (ServerSocket listener = new ServerSocket(localPort)) {
                Logger.print(Tag.CLIENT, "Listening to incoming connections on port " + localPort);

                while (expectedConnections > 0) {
                    Socket socket = listener.accept();

                    Logger.print(Tag.CLIENT, "Attempting to accept a connection from an unknown remote peer");

                    localPeerManager.connectToPeer(socket).start();

                    expectedConnections--;
                }
            }
        } catch (IOException exception) {
            System.err.println("An error occurred when trying to accept a connection from a remote peer");
            System.exit(1);
        }
    }
}