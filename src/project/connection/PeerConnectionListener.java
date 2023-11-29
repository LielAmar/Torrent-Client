package project.connection;

import project.LocalPeerManager;
import project.message.Message;
import project.message.packet.Packet;
import project.message.packet.packets.*;
import project.utils.Logger;
import project.utils.Tag;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

public class PeerConnectionListener extends PeerConnection {

    private final BlockingQueue<Message> messageQueue;

    private DataInputStream in;

    public PeerConnectionListener(Socket connection, LocalPeerManager localPeerManager,
                                  ConnectionState state, BlockingQueue<Message> incomingMessageQueue) {
        super(connection, localPeerManager, state);

        this.messageQueue = incomingMessageQueue;
    }

    public void run() {
        try {
            this.in = new DataInputStream(this.connection.getInputStream());

            // Listen to the first incoming message (handshake)
            byte[] message = this.readBytes(HandshakePacket.HANDSHAKE_LENGTH);
            this.messageQueue.put(new HandshakePacket(message));

            // Wait for the handshake to be finished
            this.state.waitForHandshake();
            this.state.unlockHandshake();

            // Start listening to incoming messages until the connection is closed
            while (this.state.isConnectionActive()) {
                this.messageQueue.put(this.listenToMessage());
            }
        } catch (IOException | InterruptedException exception) {
            System.err.println("An error occurred when listening to incoming packets with peer " +
                    this.state.getRemotePeerId());
        } finally {
            try {
                Logger.print(Tag.LISTENER, "Closing input stream with peer " + this.state.getRemotePeerId());

                this.in.close();
            } catch(IOException ioException){
                System.err.println("An error occurred when closing the input stream with peer " +
                        this.state.getRemotePeerId());
            }
        }
    }


    /**
     * Listen to a single incoming message.
     * This is done by reading 4 bytes, which is the predefined number of bytes for the length header,
     * and then reading ${lengthHeader} bytes where length is the 4-byte value read.
     *
     * @return   Receive, parsed packet. Unknown if an unknown packet was received.
     */
    private Packet listenToMessage() {
        // Read the length of the incoming packet
        byte[] lengthHeaderBytes = this.readBytes(4);
        int lengthHeader = lengthHeaderBytes == null ? 0 : ByteBuffer.wrap(lengthHeaderBytes).getInt();

        if(lengthHeader < 1) {
            return new UnknownPacket();
        }

        // Read 'lengthHeader' bytes, which is the content of the packet
        byte[] payload = this.readBytes(lengthHeader);

        // Create packet from the read payload
        Packet packet = Packet.PacketFromBytes(payload);

        Logger.print(Tag.LISTENER, "Parsed packet of type " + packet.getTypeString() + " from peer " +
                this.state.getRemotePeerId());

        return packet;
    }

    /**
     * Reads ${length} amount of bytes from the input stream
     *
     * @param length   Number of bytes to read
     * @return         Read byte array
     */
    private byte[] readBytes(int length) {
        try {
            if(length > 0) {
                byte[] message = new byte[length];
                this.in.readFully(message, 0, message.length);
                return message;
            }
        } catch(IOException exception) {
//            System.out.println("[DEBUG] Tried to read and failed");
            // try {
            //     Thread.sleep(10000);
            // } catch(InterruptedException exc) {
            //     exc.printStackTrace();
            // }
        }

        return null;
    }
}
