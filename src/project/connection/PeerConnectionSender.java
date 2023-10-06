package project.connection;

import project.PeerProcess;
import project.packet.packets.HandshakePacket;
import project.peer.Peer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class PeerConnectionSender extends PeerConnection {

    private DataOutputStream out;

    public PeerConnectionSender(Socket connection, Peer peer) {
        super(connection, peer);
    }

    public void run() {
        try {
            this.out = new DataOutputStream(this.connection.getOutputStream());

            sendHandshake();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                System.out.println("[SENDER] Closing connection with peer " + this.peer.getPeerId());

                this.out.close();
                this.connection.close();
            } catch(IOException ioException){
                System.out.println("[SENDER] Failed to close connection with peer " + this.peer.getPeerId());
            }
        }
    }

    private void sendHandshake() {
        System.out.println("[SENDER] Sending Handshake Packet");

        HandshakePacket packet = new HandshakePacket();

        packet.setPeerId(PeerProcess.config.getProcessPeerId());

        this.sendBytes(packet.build());

        System.out.println("[SENDER] Sent Handshake Packet");
    }

    private void sendBytes(byte[] payload) {
        try{
            out.write(payload);
            out.flush();

            System.out.println("[SENDER] Sent message: " + payload + " to peer " + this.peer.getPeerId());
        } catch(IOException ioException){
            ioException.printStackTrace();
        }
    }
}
