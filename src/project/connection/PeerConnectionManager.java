package project.connection;

import java.net.Socket;

import project.PeerProcess;
import project.exceptions.NetworkException;
import project.packet.Packet;
import project.packet.PacketType;
import project.packet.packets.*;
import project.peer.Peer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PeerConnectionManager extends PeerConnection {

    private final BlockingQueue<Packet> incomingMessageQueue;
    private final BlockingQueue<Packet> outgoingMessageQueue;

    private final PeerConnectionSender sender;
    private final PeerConnectionListener listener;

    public PeerConnectionManager(Socket connection, ConnectionState state) {
        super(connection, state);

        this.incomingMessageQueue = new LinkedBlockingQueue<>();
        this.outgoingMessageQueue = new LinkedBlockingQueue<>();

        this.sender = new PeerConnectionSender(connection, state, this.outgoingMessageQueue);
        this.listener = new PeerConnectionListener(connection, state, this.incomingMessageQueue);
    }

    public void run() {
        super.state.lockHandshake();

        this.sender.start();
        this.listener.start();

        try {
            // Create handshake packet to send and listen to handshake packet
            this.outgoingMessageQueue.put(new HandshakePacket(super.state.getLocalId()));
            Packet receivedPacket = this.incomingMessageQueue.take();

            if(receivedPacket.getType() != PacketType.HANDSHAKE) {
                System.out.println("[MANAGER] Handshake not received as first message");
                throw new NetworkException("[MANAGER] Handshake not received as first message");
            }

            HandshakePacket handshake = (HandshakePacket) receivedPacket;
            
            if(!handshake.isValid()) {
                System.out.println("[MANAGER] Received invalid handshake");
                throw new NetworkException("[MANAGER] Received an invalid handshake");
            }

            System.out.println("[MANAGER] Handshake received, starting listening");
            super.state.setPeerId(handshake.getPeerId());

            super.state.unlockHandshake();

            // Create bitfield packet to send
            System.out.println("creating bitfield packet:");
            BitFieldPacket bitFieldPacket = new BitFieldPacket();
            bitFieldPacket.setPayload(PeerProcess.config.piecesStatusToBitset());
            this.outgoingMessageQueue.put(bitFieldPacket);

            // Use the manager to listen to incoming messages and update peer connection data
            while(super.state.getConnectionActive()) {
                receivedPacket = this.incomingMessageQueue.take();

                handleReceivedPacket(receivedPacket);
            }
        } catch (InterruptedException | NetworkException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleReceivedPacket(Packet packet) {
        switch (packet.getType()) {
            case CHOKE -> {}
            case UNCHOKE -> {}
            case INTERESTED -> {
//                send have
            }
            case NOT_INTERESTED -> {}
            case HAVE -> {
//
            }
            case BITFIELD -> receivedBitfield((BitFieldPacket) packet);
            case REQUEST -> {}
            case PIECE -> {}
            case HANDSHAKE -> {}
            default -> {}
        }
    }

    private void receivedBitfield(BitFieldPacket packet) {
        System.out.println("bitfield packet payload length: " + packet.getPayload().length());
        this.state.getPeer().setBitfield(packet.getPayload());
    }
}
