import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.net.Socket;
import java.net.ServerSocket;
import java.nio.file.*;

public class PeerProcess {

    private final static String COMMON_CONFIG_FILE = "Common.cfg";
    private final static String PEER_INFO_CONFIG_FILE = "PeerInfo.cfg";

    private static int sPort; //The server will be listening on this port number
    private static int localPeerId;
    private static PeerConnection CreateClientConnection(int peerId, String hostname, int port)
    {
        Socket requestSocket = null;
        PeerConnection newConnection = null;
        try
        {
            requestSocket = new Socket(hostname, port);
            newConnection = new PeerConnection(requestSocket, new Peer(peerId), localPeerId);
            newConnection.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        return newConnection;
    }

    private static void ReceiveClientConnections()
    {
        System.out.println("Listening to connections from peers on port " + sPort);
        try
        {
            ServerSocket listener = new ServerSocket(sPort);
            try {
                while(true) {
                    new PeerConnection(listener.accept(), new Peer(-1), localPeerId).start();
                    System.out.println("Peer is connected!");
                }
            } finally {
                listener.close();
            }   
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Please specify a peerId!");
            return;
        }

        int peerId = Integer.parseInt(args[0]);
        localPeerId = peerId;

        System.out.println("Peer ID: " + peerId);
        CommonConfiguration config = readCommonFile();
        System.out.println(config.toString());

        // Read peer info to get our port
        List<PeerConnection> connectionThreads = readPeerInfoFile(peerId);

        ReceiveClientConnections();
    }

    // remove this function, just using it for some testing with weird inputs
    public static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\f", "\\f")
                .replace("\'", "\\'") // <== not necessary
                .replace("\"", "\\\"");
    }
      
    private static CommonConfiguration readCommonFile() {
        CommonConfiguration config;

        // No idea why this happens.
        // It cant find the file despite it being at the path its looking
        // but when you take the absolute path it can find it, so im just going to leave it

        // File f = new File(COMMON_CONFIG_FILE);
        // System.out.println("Abs path: " + f.getAbsolutePath());
        // System.out.println("Exists: " + f.exists());       

        // System.out.println("Path: " + f.getPath());     

        try (BufferedReader br = new BufferedReader(new FileReader((new File(COMMON_CONFIG_FILE)).getAbsolutePath()))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }

            // String[] rows = sb.toString().split("\n");
            // System.out.println(rows.length);
            // System.out.println(escape(sb.toString()));
            // for(int i = 0; i < rows.length; i++)
            // {
            //     System.out.println(rows[i]);
            //     System.out.println(escape(rows[i]));
            // }

            // remove all \r from the string, as this breaks 
            // the integer conversion
            while(sb.indexOf("\r") != -1)
            {
                sb.deleteCharAt(sb.indexOf("\r"));
            }

            // split the converted string by row
            String[] rows = sb.toString().split("\n");
            // System.out.println(rows2.length);
            // System.out.println(escape(sb.toString()));
            // for(int i = 0; i < rows2.length; i++)
            // {
            //     System.out.println(rows2[i]);
            //     System.out.println(escape(rows2[i]));
            // }

            
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
    private static List<PeerConnection> readPeerInfoFile(int pid) {
        List<PeerConnection> peerConnections = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader((new File(PEER_INFO_CONFIG_FILE)).getAbsolutePath()))) 
        {
            String line;

            while ((line = br.readLine()) != null) {
                String[] values = line.split(" ");

                if (!(Integer.parseInt(values[0]) == pid)) 
                {
                    System.out.println("Creating connection to peerId: " + values[0]);
                    peerConnections.add(CreateClientConnection(Integer.parseInt(values[0]), values[1], Integer.parseInt(values[2])));
                } 
                else 
                {
                    sPort = Integer.parseInt(values[2]);
                    return peerConnections;
                }
            }
        } 
        catch (IOException exception) 
        {
            exception.printStackTrace();
        }

        return null;
    }
}
