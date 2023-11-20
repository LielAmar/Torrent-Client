package project.connection;

import project.packet.Packet;
import project.packet.packets.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

public class PeerConnectionListener extends PeerConnection {

    private final BlockingQueue<Packet> messageQueue;

    private DataInputStream in;

    public PeerConnectionListener(Socket connection, ConnectionState state, BlockingQueue<Packet> incomingMessageQueue) {
        super(connection, state);

        this.messageQueue = incomingMessageQueue;
    }

    public void run() {
        try {
            // Get the input stream
            this.in = new DataInputStream(this.connection.getInputStream());

            // Listen to the first incoming message (handshake)
            this.messageQueue.put(listenToHandshake());

            // Wait for the handshake to be finished
            this.state.waitForHandshake();
            this.state.unlockHandshake();

            // Start listening to incoming messages
            while (this.state.isConnectionActive()) {
                this.messageQueue.put(this.listenToMessage());
            }
        } catch (IOException | InterruptedException e) {
//            throw new RuntimeException(e);
        } finally {
            try {
                System.out.println("[LISTENER] Closing input stream with peer " + this.state.getRemotePeerId());

                this.in.close();
            } catch(IOException ioException){
                System.out.println("[LISTENER] Failed to close input stream with peer " + this.state.getRemotePeerId());
            }
        }
    }

    private byte[] readBytes(int length) {
        try {
            if(length > 0) {
                // System.out.println("[DEBUG] Trying to read " + length + " bytes");
                byte[] message = new byte[length];
                this.in.readFully(message, 0, message.length);
                return message;
            }
        } catch(IOException exception) {
            System.out.println("[DEBUG] Tried to read and failed");
        }

        return null;
    }

    private Packet listenToMessage() {
        // TODO: check if there's a message in the input stream
        // Matthew: why do we need to check if there is a message waiting? if there isnt, then this will just block
        // and since this is in its own dedicated thread, that is fine, right?

        // Read the length of the incoming packet
        byte[] lengthHeaderBytes = this.readBytes(4);
        int lengthHeader = lengthHeaderBytes == null ? 0 : ByteBuffer.wrap(lengthHeaderBytes).getInt();

        if(lengthHeader < 1) {
            return new UnknownPacket();
        }

        // Read 'lengthHeader' bytes, which is the content of the packet
        byte[] payload = this.readBytes(lengthHeader);

        // Create packet from the read payload
        return Packet.PacketFromBytes(payload);
    }

    private Packet listenToHandshake() {
        System.out.println("[LISTENER] Listening to Handshake Packet");
        byte[] message = this.readBytes(HandshakePacket.HANDSHAKE_LENGTH);
        return new HandshakePacket(message);
    }
}
