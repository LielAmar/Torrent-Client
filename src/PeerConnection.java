import exceptions.NetworkException;
import message.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.LinkedList;

public class PeerConnection extends Thread {

    private static final int HANDSHAKE_LENGTH = 32;
    private static final String HANDSHAKE_HEADER = "P2PFILESHARINGPROJ";
    private static final int HANDSHAKE_ZERO_BITS_LENGTH = 10;

    private final Socket connection;
    private final Peer peer;

    private final Deque<Message> incomingMessageQueue;

    private InputStream in;
    private OutputStream out;

    public PeerConnection(Socket connection, Peer peer) {
        this.connection = connection;
        this.peer = peer;

        this.incomingMessageQueue = new LinkedList<>();
    }

    public void run() {
        int peerId;
        
        try {
            this.in = this.connection.getInputStream();
            this.out = this.connection.getOutputStream();

            // TODO: make sure the handshake is correct.
            // Currently, both sides send handshake as soon as possible, but it might cause issues (if both send in the same type)
            // and are unable to listen or something like that.
            // Might wanna figure out a way to decide on a first sender and a second sender (client-created vs server-created in main)
            System.out.println("[CONNECTION] Sending handshake to peer " + this.peer.getPeerId());
            sendHandshake();

            peerId = listenToHandshake();
            if(peerId <= 0) {
                System.out.println("[CONNECTION] Handshake with " + this.peer.getPeerId() + " failed, closing connection");
                return; // Still executes finally statement
            }

            this.peer.setPeerId(peerId);
            System.out.println("[CONNECTION] Handshake received from peer " + this.peer.getPeerId());

            // TODO: override this
            while (true) {
                listenToMessages();
                while(!this.incomingMessageQueue.isEmpty()) {
                    System.out.println("Processing message");
                    this.incomingMessageQueue.pop();
                }

                // TODO: determine if we need to send any messages
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                System.out.println("[CONNECTION] Closing connection with peer " + this.peer.getPeerId());

                this.in.close();
                this.out.close();
                this.connection.close();
            } catch(IOException ioException){
                System.out.println("[CONNECTION] Failed to close connection with peer " + this.peer.getPeerId());
            }
        }
    }

    private void sendMessage(Message message) throws NetworkException {
        // TODO: add logging here
        byte[] messageBuffer = message.getFullMessage();

        try {
            this.out.write(messageBuffer);
        } catch(IOException e) {
            System.out.println("[CONNECTION] Failed to send a message to peer " + this.peer.getPeerId());
        }
    }

    private void listenToMessages() {
        
    }
    
    private void sendHandshake() {
        byte[] buffer = new byte[HANDSHAKE_LENGTH];

        // Set the handshake header
        int i;
        for (i = 0; i < HANDSHAKE_HEADER.length(); i++) {
            buffer[i] = (byte) HANDSHAKE_HEADER.charAt(i);
        }

        // Set the handshake zero bits (10 zero bytes)
        i += HANDSHAKE_ZERO_BITS_LENGTH;

        // Set the peer id
        System.arraycopy(ByteBuffer.allocate(4).putInt(PeerProcess.config.getProcessPeerId()).array(), 0,
                buffer, i, 4);

        try {
            this.out.write(buffer);
        } catch(IOException e) {
            System.out.println("[CONNECTION] Failed to send handshake to peer " + this.peer.getPeerId());
        }
    }

    /**
     * Listens to the input stream and tries to decode the handshake packet
     *
     * @return   The ID of the connected peer
     */
    private int listenToHandshake() {
        byte[] buff = new byte[HANDSHAKE_LENGTH];
        int i = 0;

        try {
            while(i < 32) {
                i += this.in.read(buff, i, HANDSHAKE_LENGTH - i);
            }
        } catch (IOException e) {
            System.out.println("[CONNECTION] Failed to receive handshake from peer " + this.peer.getPeerId());
            return -1;
        }

        // Verify header
        String verifyHeader = new String(buff, 0, HANDSHAKE_HEADER.length());
        if (!(verifyHeader.equals(HANDSHAKE_HEADER))) {
            System.out.println("[CONNECTION] Handshake header received from " + this.peer.getPeerId() + " is not valid");
            return -1;
        }

        // Verify 0 bytes
        for (int j = HANDSHAKE_HEADER.length(); j < HANDSHAKE_HEADER.length() + HANDSHAKE_ZERO_BITS_LENGTH; j++) {
            if (buff[j] != 0) {
                System.out.println("[CONNECTION] Handshake zero bytes missing from peer " + this.peer.getPeerId());
                return -1;
            }
        }

        // Decode peer ID
        ByteBuffer peerIdByteBuffer = ByteBuffer.allocate(4).put(buff, 28, 4);
        peerIdByteBuffer.rewind();
        return peerIdByteBuffer.getInt();
    }
}
