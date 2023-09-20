import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.net.Socket;

public class PeerProcess {

    private final String COMMON_CONFIG_FILE = "Common.cfg";
    private final String PEER_INFO_CONFIG_FIL = "PeerInfo.cfg";

    public PeerConnection CreateClientConnection(String peerId, String hostname, int port)
    {
        Socket requestSocket = null;
        PeerConnection newConnection = null;
        try
        {
            requestSocket = new Socket(hostname, port);
            newConnection = new PeerConnection(requestSocket, new Peer(peerId));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return newConnection;
    }

    public void ReceiveClientConnections(String pid, String hostname, int port)
    {

    }

    public void main(String[] args) {
        if(args.length != 1) {
            System.err.println("Please specify a peerId!");
            return;
        }

        String peerId = args[0];

        CommonConfiguration config = readCommonFile();

        // Read peer info to get our port
    }

    private CommonConfiguration readCommonFile() {
        CommonConfiguration config;
        try (BufferedReader br = new BufferedReader(new FileReader(COMMON_CONFIG_FILE))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }

            String[] rows = sb.toString().split("\n");
            config = new CommonConfiguration(
                    Integer.parseInt(rows[0].split(" ")[1]), // number of preferred neighbors
                    Integer.parseInt(rows[1].split(" ")[1]), // unchoking interval
                    Integer.parseInt(rows[2].split(" ")[1]), // optimistic unchoking interval
                    rows[3].split(" ")[1],                   // file name
                    Long.parseLong(rows[4].split(" ")[1]),   // file size
                    Long.parseLong(rows[5].split(" ")[1])    // piece size
            );
        } catch (IOException exception) {
            exception.printStackTrace();
            return null;
        }

        return config;
    }

    /**
     * Parses the peerInfo.cfg file and opens connections with every peer that comes before
     * the peer with id ${pid}.
     *
     * @param pid   ID of the current peer
     * @return      List of peers
     */
    private List<PeerConnection> readPeerInfoFile(String pid) {
        List<PeerConnection> peerConnections = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(PEER_INFO_CONFIG_FIL))) {
            String line;

            while ((line = br.readLine()) != null) {
                String[] values = line.split(" ");

                if(!values[0].equals(pid)) {
                    peerConnections.add(CreateClientConnection(pid, values[1], Integer.parseInt(values[2])));
                } else {
                    ReceiveClientConnections(pid, values[1], Integer.parseInt(values[2]));

                    return peerConnections;
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return null;
    }
}
