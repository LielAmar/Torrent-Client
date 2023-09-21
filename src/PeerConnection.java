import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.text.html.parser.Element;

public class PeerConnection extends Thread {

    private enum MessageType {
        choke(0),
        unchoke(1),
        interested(2),
        notinterested(3),
        have(4),
        bitfield(5),
        request(6),
        piece(7);

        public final int value;

        private MessageType(int value) {
            this.value = value;
        }
    }


    private class Message {

    }

    static final String handshakeHeader = "P2PFILESHARINGPROJ";
    

    private Peer peer;
    private int localPeerId;
    private Socket connection;
    private InputStream in;
    private OutputStream out;
    private LinkedList<Message> incomingMessageQueue;

    public PeerConnection(Socket connection, Peer peer, int localPeerId)
    {
        this.connection = connection;
        this.peer = peer;
        this.localPeerId = localPeerId;
        incomingMessageQueue = new LinkedList<Message>();
    }

    public void run()
    {
        int peerId;
        try
        {
            in = connection.getInputStream();
            out = connection.getOutputStream();
            System.out.println("Connection established with peer " + peer.getPeerId());
            SendHandshake();
            peerId = ListenHandshake();
            if (peerId <= 0)
            {
                System.out.println("Handshake failed, closing connection");
                return;
            }
            peer.setPeerId(peerId);
            System.out.println("Handshake received from peer " + peer.getPeerId());
            while (true) 
            {
                ListenMessage();
                while(!incomingMessageQueue.isEmpty())
                {
                    System.out.println("Processing message");
                    incomingMessageQueue.pop();
                }

                // determine if we need to send any messages
            }
        }
        catch (IOException e)
        {
            System.out.println("Disconnect with Client " + peer.getPeerId());
        }
        finally{
            //Close connections
            try{
                in.close();
                out.close();
                connection.close();
            }
            catch(IOException ioException){
                System.out.println("Disconnect with Client " + peer.getPeerId() + " failed");
            }
        }
    }
    
    private void sendMessage(MessageType type, String data)
    {
        
    }

    private void sendMessage(MessageType type)
    {
        sendMessage(type, null);
    }


    private void ListenMessage()
    {

    }
    
    private void SendHandshake()
    {
        byte[] buff = new byte[32];
        
        int i;
        for (i = 0; i < handshakeHeader.length(); i++) 
        {
            buff[i] = (byte) handshakeHeader.charAt(i);
        }
        // leave 10 zero bytes
        i += 10;

        //System.out.println("Local Peer ID" + localPeerId + "\nHex: " + Integer.toHexString(localPeerId));
        ByteBuffer localPeerIdByteBuffer = ByteBuffer.allocate(4).putInt(localPeerId);
        for(int j = 0; j < 4; j++, i++)
        {
            buff[i] = localPeerIdByteBuffer.array()[j];
        }
        
        try
        {
            out.write(buff);
        }
        catch(IOException e)
        {
            System.out.println("Falied to send handshake.");
        }
    }

    private int ListenHandshake()
    {
        final int handshakeLen = 32;
        byte[] buff = new byte[32];
        int i = 0;
        try
        {
            while(i < 32)
            {
                i += in.read(buff, i, handshakeLen - i);
                //System.out.println("read " + j + "bytes of handshake");
            }
        }
        catch (IOException e)
        {
            System.out.println("Falied to recieve handshake.");
            return -1;
        }

        // print out the raw bytes of the handshake, just left in case needed for debugging
        // System.out.println("Raw handshake received:");
        // for (byte b1 : buff) {
        //     System.out.format("0x%x ", b1);
        // }
        // System.out.print("\n");


        // Decode Handshake

        // verify header    
        String verifyHeader = new String(buff, 0, handshakeHeader.length());
        if (!(verifyHeader.equals(handshakeHeader)))
        {
            System.out.println("Handshake header not valid");
            return -1;
        }
        // verify 0 bytes
        for (int j = handshakeHeader.length(); j < handshakeHeader.length() + 10; j++)
        {
            if (buff[j] != 0) {
                System.out.println("Handshake zero bytes missing");
                return -1;
            }
        }
        //decode peerId
        int peerId = 0;
        ByteBuffer peerIdByteBuffer = ByteBuffer.allocate(4).put(buff, 28, 4);
        peerIdByteBuffer.rewind();
        peerId = peerIdByteBuffer.getInt();

        return peerId;
    }
}
