package project.connection;

import project.LocalPeerManager;
import project.exceptions.NetworkException;
import project.message.packet.Packet;
import project.utils.Logger;
import project.utils.Tag;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class PeerConnectionSender extends PeerConnection {

    private final BlockingQueue<Packet> messageQueue;

    private OutputStream out;

    private final LocalPeerManager localPeerManager;

    public PeerConnectionSender(Socket connection, LocalPeerManager localPeerManager,
                                ConnectionState state, BlockingQueue<Packet> outgoingMessageQueue) {
        super(connection, localPeerManager, state);

        this.messageQueue = outgoingMessageQueue;
        this.localPeerManager = localPeerManager;
    }

    public void run() {
        try {
            this.out = this.connection.getOutputStream();

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
            System.err.println(exception);
            exception.printStackTrace();
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
        if (!this.state.isConnectionActive()) {
            return;
        }

        try {
            Logger.print(Tag.SENDER, "Attempting to send a message of type " + message.getTypeString() +
                    " to peer " + this.state.getRemotePeerId() + ". Data: " + message.dataString());
            byte[] messageBytes = message.build();
            this.out.write(messageBytes);
            this.out.flush();

        } catch (NetworkException exception) {
            System.err.println("An error occurred when building a message of type " + message.getTypeString() +
                    " to send to peer " + this.state.getRemotePeerId());
        } catch (IOException exception) {
            // if we get to an error here, its because the remote connection closed, so we probably lost a have message somewhere, and we should probably close too
            this.localPeerManager.dumpFile();
            this.localPeerManager.getLogger().close();
            System.exit(0);
            //System.err.println("An error occurred whens sending a message of type " + message.getTypeString() +
            //        " to peer " + this.state.getRemotePeerId());
            //Logger.print(Tag.SENDER, "An error occurred whens sending a message of type " + message.getTypeString() +
            //        " to peer " + this.state.getRemotePeerId() + ". " + exception.toString());
            //System.err.println(exception);
            //exception.printStackTrace();
        }
    }
    
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

}
