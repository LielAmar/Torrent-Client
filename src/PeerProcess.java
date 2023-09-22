import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class PeerProcess {

    private final static String COMMON_CONFIG_FILE = "Common.cfg";
    private final static String PEER_INFO_CONFIG_FILE = "PeerInfo.cfg";

    public static Configuration config;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Please specify a peerId!");
            return;
        }

        setupConfiguration(Integer.parseInt(args[0]));
        setupPeerConnections();
    }


    /**
     * Parses the Common.cfg file and sets up the configuration
     *
     * @param peerId   The ID of the local peer
     */
    private static void setupConfiguration(int peerId) {
        try (BufferedReader br = new BufferedReader(new FileReader(COMMON_CONFIG_FILE))) {
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
                    peerId,                                        // Process peer id
                    Integer.parseInt(rows[0].split(" ")[1]), // number of preferred neighbors
                    Integer.parseInt(rows[1].split(" ")[1]), // unchoking interval
                    Integer.parseInt(rows[2].split(" ")[1]), // optimistic unchoking interval
                    rows[3].split(" ")[1],                   // file name
                    Integer.parseInt(rows[4].split(" ")[1]), // file size
                    Integer.parseInt(rows[5].split(" ")[1])  // piece size
            );
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Parses the peerInfo.cfg file, opens connections with previous peers & starts listening to incoming connections.
     */
    private static void setupPeerConnections() {
        try (BufferedReader br = new BufferedReader(new FileReader(PEER_INFO_CONFIG_FILE))) {
            String line;

            while ((line = br.readLine()) != null) {
                String[] values = line.split(" ");

                int peerId = Integer.parseInt(values[0]);
                int port = Integer.parseInt(values[2]);

                // Open connections as long as didn't encounter a line for the current peer id
                if (peerId != config.getProcessPeerId()) {
                    String hostname = values[1];

                    connectToPeer(peerId, hostname, port);
                } else {
                    // Start listening to incoming connections with next peers.
                    listenToIncomingConnections(port);
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Connects to another peer
     *
     * @param peerId     ID of the peer to connect to
     * @param hostname   Hostname of the peer to connect to
     * @param port       Port of the peer to connect to
     */
    private static void connectToPeer(int peerId, String hostname, int port) {
        try {
            System.out.println("[CLIENT] Creating connection to peerId: " + peerId + " at host " + hostname + ":" + port);
            new PeerConnection(new Socket(hostname, port), new Peer(peerId)).start();
        } catch (IOException e) {
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
                    System.out.println("[SERVER] Received a connection request from another peer!");
                    // TODO: figure out a way to extract the peer id instead of inputting -1
                    new PeerConnection(listener.accept(), new Peer(-1)).start();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
