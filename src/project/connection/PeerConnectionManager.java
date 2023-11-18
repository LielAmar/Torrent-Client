package project.connection;

import java.net.Socket;

import project.PeerProcess;
import project.connection.piece.Piece;
import project.connection.piece.PieceStatus;
import project.exceptions.NetworkException;
import project.packet.Packet;
import project.packet.PacketType;
import project.packet.packets.*;

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
            this.outgoingMessageQueue.put(new HandshakePacket(super.state.getLocalPeerId()));
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


            // Send first bitfield packet
            this.sendBitfield();


            // Use the manager to listen to incoming messages and update peer connection data
            while(super.state.isConnectionActive()) {
                if(!this.state.isChoked()) { // TODO: might wanna change to semaphore to make thread sleep
                    receivedPacket = this.incomingMessageQueue.take();

                    handleReceivedPacket(receivedPacket);
                }
            }
        } catch (InterruptedException | NetworkException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Handle receiving and parsing a packet
     *
     * @param packet   Packet received
     */
    private void handleReceivedPacket(Packet packet) {
        System.out.println("[HANDLER (" + this.state.getRemotePeerId() + ")] Received a " + packet.getTypeString() + " packet");
        switch (packet.getType()) {
            case CHOKE -> {}
            case UNCHOKE -> handleReceivedUnchoke();
            case INTERESTED -> {
//              send have
            }
            case NOT_INTERESTED -> {}
            case HAVE -> handleReceivedHave((HavePacket)packet);
            case BITFIELD -> handleReceivedBitfield((BitFieldPacket) packet);
            case REQUEST -> handleReceivedRequest((RequestPacket) packet);
            case PIECE -> handleReceivedPiece((PiecePacket) packet);
            default -> {}
        }
    }


    /**
     * Handle receiving and parsing a bitfield packet, and then preparing next packet to be sent
     * - The next packet to be sent is either INTERESTED or NOT_INTERESTED
     *
     * @param packet   Packet received
     */
    private void handleReceivedBitfield(BitFieldPacket packet) {
        PieceStatus[] pieces = PieceStatus.bitsetToPiecesStatus(packet.getBitfield(), PeerProcess.config.getNumberOfPieces());
        this.state.setPieces(pieces);

        // send next packet
        this.sendInterestedNotInterested();
    }

    private void handleReceivedRequest(RequestPacket packet) {
        int pieceIndex = packet.getPieceIndex();

        // TODO : check choke somewhere and if chocked don't send
        // TODO: also check and make sure local does have the content of the piece
        byte[] pieceContent = PeerProcess.localPeerManager.getLocalPieces()[pieceIndex].getContent();

        try {
            this.sendPiece(pieceIndex, pieceContent);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleReceivedPiece(PiecePacket packet) {
        int pieceIndex = packet.getPieceIndex();
        byte [] pieceContent = packet.getPieceContent();

        PeerProcess.localPeerManager.setLocalPiece(pieceIndex, PieceStatus.HAVE, pieceContent);

        try {
            this.sendHave(pieceIndex); // TODO: send have to everyone
            this.sendRequest();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleReceivedHave(HavePacket packet) {
        int pieceIndex = packet.getPieceIndex();

        this.state.updatePiece(pieceIndex);

        this.sendInterestedNotInterested();
    }

    private void handleReceivedUnchoke() {
        try {
            this.sendRequest();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Checks if exists a piece that the local piece doesn't have but the remote does.
     * If so, send a request message to that packet.
     */
    private void sendInterestedNotInterested() {
        Piece[] local = PeerProcess.localPeerManager.getLocalPieces();
        PieceStatus[] remote = this.state.getPieces();

        for(int pieceId = 0; pieceId < Math.min(local.length, remote.length); pieceId++) {
            if(local[pieceId].getStatus() == PieceStatus.NOT_HAVE && remote[pieceId] == PieceStatus.HAVE) {
                try {
                    this.sendInterested();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return;
            }
        }

        try {
            this.sendNotInterested();
            this.sendUnchoke(); // TODO: remove this
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }



    private void sendBitfield() throws InterruptedException {
        System.out.println("[HANDLER (" + this.state.getRemotePeerId() + ")] Preparing Bitfield packet to send");

        BitFieldPacket bitFieldPacket = new BitFieldPacket();
        bitFieldPacket.setData(PieceStatus.piecesToBitset(PeerProcess.localPeerManager.getLocalPieces()));
        this.outgoingMessageQueue.put(bitFieldPacket);
    }

    private void sendInterested() throws InterruptedException {
        System.out.println("[HANDLER (" + this.state.getRemotePeerId() + ")] Preparing Interested packet to send");

        Packet packet = new InterestedPacket();
        this.outgoingMessageQueue.put(packet);
    }

    private void sendNotInterested() throws InterruptedException {
        System.out.println("[HANDLER (" + this.state.getRemotePeerId() + ")] Preparing Not Interested packet to send");

        Packet packet = new NotInterestedPacket();
        this.outgoingMessageQueue.put(packet);
    }

    private void sendPiece(int pieceIndex, byte[] piece) throws InterruptedException {
        System.out.println("[HANDLER (" + this.state.getRemotePeerId() + ")] Preparing Piece packet to send");

        PiecePacket packet = new PiecePacket();
        packet.setData(pieceIndex, piece);
        this.outgoingMessageQueue.put(packet);
    }

    private void sendHave(int pieceIndex) throws InterruptedException {
        System.out.println("[HANDLER (" + this.state.getRemotePeerId() + ")] Preparing Have packet to send");

        HavePacket packet = new HavePacket();
        packet.setData(pieceIndex);
        this.outgoingMessageQueue.put(packet);
    }

    private void sendChoke() throws InterruptedException {
        System.out.println("[HANDLER (" + this.state.getRemotePeerId() + ")] Preparing Choke packet to send");

        ChokePacket packet = new ChokePacket();
        this.outgoingMessageQueue.put(packet);
    }

    private void sendUnchoke() throws InterruptedException {
        System.out.println("[HANDLER (" + this.state.getRemotePeerId() + ")] Preparing Unchoke packet to send");

        UnchokePacket packet = new UnchokePacket();
        this.outgoingMessageQueue.put(packet);
    }

    private void sendRequest() throws InterruptedException {
        System.out.println("[HANDLER (" + this.state.getRemotePeerId() + ")] Preparing Request packet to send");

        RequestPacket packet = new RequestPacket();

        // TODO: change this to be a random piece local doesn't have instead of first piece that local doesn't have.
        int desiredPieceIndex = -1;
        for(int i = 0; i < this.state.getPieces().length; i++) {
            // If remote has a piece that local doesn't, ask for this piece
            if(this.state.getPieces()[i] == PieceStatus.HAVE &&
                    PeerProcess.localPeerManager.getLocalPieces()[i].getStatus() == PieceStatus.NOT_HAVE) {
                desiredPieceIndex = i;
                break;
            }
        }

        packet.setData(desiredPieceIndex);

        if(desiredPieceIndex != -1) {
            this.outgoingMessageQueue.put(packet);
        }
    }
}
