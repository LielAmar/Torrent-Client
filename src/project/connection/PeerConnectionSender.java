package project.connection;

import project.LocalPeerManager;
import project.exceptions.NetworkException;
import project.message.packet.Packet;
import project.utils.Logger;
import project.utils.Tag;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class PeerConnectionSender extends PeerConnection {

    private final BlockingQueue<Packet> messageQueue;

    private DataOutputStream out;

    public PeerConnectionSender(Socket connection, LocalPeerManager localPeerManager,
                                ConnectionState state, BlockingQueue<Packet> outgoingMessageQueue) {
        super(connection, localPeerManager, state);

        this.messageQueue = outgoingMessageQueue;
    }

    public void run() {
        try {
            this.out = new DataOutputStream(this.connection.getOutputStream());

            // Send the first outgoing message (handshake)
            Packet packet = this.messageQueue.take();
            sendMessage(packet);

            // Wait for the handshake to be finished
            this.state.waitForHandshake();
            this.state.unlockHandshake();

            // Send the second outgoing message (bitfield)
            packet = this.messageQueue.take();
            sendMessage(packet);

            // Start sending outgoing messages until the connection is closed
            while (super.state.isConnectionActive()) {
                this.sendMessage(this.messageQueue.take());
            }
        } catch (IOException | InterruptedException exception) {
            System.err.println("An error occurred when sending outgoing packets with peer " +
                    this.state.getRemotePeerId());
        } finally {
            try {
                Logger.print(Tag.SENDER, "Closing output stream with peer " + this.state.getRemotePeerId());

                this.out.close();
            } catch(IOException ioException){
                System.err.println("An error occurred when closing the output stream with peer " +
                        this.state.getRemotePeerId());
            }
        }
    }

    private void sendMessage(Packet message) {
        if(!this.state.isConnectionActive()) {
            return;
        }

        try {
            Logger.print(Tag.SENDER, "Attempting to send a message of type " + message.getTypeString() +
                    " to peer " + this.state.getRemotePeerId());
            byte[] messageBytes = message.build();

            this.out.write(messageBytes);
            this.out.flush();
        } catch(NetworkException exception) {
            System.err.println("An error occurred when building a message of type " + message.getTypeString() +
                    " to send to peer " + this.state.getRemotePeerId());
        } catch(IOException exception) {
            System.err.println("An error occurred whens ending a message of type " + message.getTypeString() +
                    " to peer " + this.state.getRemotePeerId());
        }
    }
}
