package project.connection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

import project.PeerProcess;
import project.connection.piece.Piece;
import project.connection.piece.PieceStatus;
import project.exceptions.NetworkException;
import project.packet.Packet;
import project.packet.PacketType;
import project.packet.packets.*;

import java.util.BitSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PeerConnectionManager extends PeerConnection {

    private final BlockingQueue<Packet> incomingMessageQueue;
    private final BlockingQueue<Packet> outgoingMessageQueue;

    private final PeerConnectionSender sender;
    private final PeerConnectionListener listener;

    private boolean bitfieldSent;

    public PeerConnectionManager(Socket connection, ConnectionState state) {
        super(connection, state);

        this.incomingMessageQueue = new LinkedBlockingQueue<>();
        this.outgoingMessageQueue = new LinkedBlockingQueue<>();

        this.sender = new PeerConnectionSender(connection, state, this.outgoingMessageQueue);
        this.listener = new PeerConnectionListener(connection, state, this.incomingMessageQueue);
        bitfieldSent = false;
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
            super.state.setRemotePeerId(handshake.getPeerId());

            super.state.unlockHandshake();


            // Send bitfield packet and start listening for have messages from other threads
            // see comment in LocalPeerManager for details.
            PeerProcess.localPeerManager.AquireBitMapLock();
            this.sendBitfield();
            this.bitfieldSent = true;
            PeerProcess.localPeerManager.ReleaseBitMapLock();

            // Use the manager to listen to incoming messages and update peer connection data
            while(super.state.isConnectionActive()) {
                this.handleReceivedPacket(this.incomingMessageQueue.take());
            }
        } catch (InterruptedException | NetworkException e) {
            this.terminate();
//            throw new RuntimeException(e);
        }
    }


    /**
     * Handle receiving and parsing a packet
     *
     * @param packet   Packet received
     */
    private void handleReceivedPacket(Packet packet) {
        // TODO: finish all handlers
        System.out.println("[HANDLER (" + this.state.getRemotePeerId() + ")] Received a " + packet.getTypeString() + " packet");

        switch (packet.getType()) {
            case CHOKE -> {}
            case UNCHOKE -> handleReceivedUnchoke();
            case INTERESTED -> handleReceivedInterested();
            case NOT_INTERESTED -> handleReceivedNotInterested();
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

        // TODO: check and make sure local does have the content of the piece
        byte[] pieceContent = PeerProcess.localPeerManager.getLocalPieces()[pieceIndex].getContent();

        // If the remote peer is not locally choked, it means the local peer can send pieces to it.
        if(!this.state.isLocalChoked()) {
            this.sendPiece(pieceIndex, pieceContent);
        }
    }

    private void handleReceivedPiece(PiecePacket packet) {
        PeerProcess.localPeerManager.setLocalPiece(packet.getPieceIndex(), PieceStatus.HAVE, packet.getPieceContent());
        this.state.addDownloadSpeed();

        // TODO: remove this code below, temporary solution to keep sending requests
        // This checks if there are more pieces to receive.
        for(int i = 0; i < PeerProcess.localPeerManager.getLocalPieces().length; i++) {
            if(PeerProcess.localPeerManager.getLocalPieces()[i].getStatus() != PieceStatus.HAVE) {
                if(!this.state.isRemoteChoke()) { // If the remote peer hasn't choked local peer, only then request
                    this.sendRequest();
                    return;
                }
            }
        }

        // If no more pieces left, dump the file
        this.dumpFile();

        // PeerProcess.localPeerManager.announce(pieceIndex); // TODO: send have to everyone
        // going to do this in the localPeerManager instead
    }

    private void handleReceivedHave(HavePacket packet) {
        int pieceIndex = packet.getPieceIndex();

        this.state.updatePiece(pieceIndex);

        // Check if this connection should be terminated and terminate it if so
        PeerProcess.localPeerManager.checkTerminateConnection(this);

        this.sendInterestedNotInterested();
    }

    private void handleReceivedUnchoke() {
        this.sendRequest();
    }

    private void handleReceivedInterested() {
        this.state.setInterested(true);
    }

    private void handleReceivedNotInterested() {
        this.state.setInterested(false);
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
                this.sendInterested();
                return;
            }
        }

        this.sendNotInterested();
        // this.sendUnchoke(); // TODO: remove this
    }

    private void sendBitfield() {
        System.out.println("[HANDLER (" + this.state.getRemotePeerId() + ")] Preparing Bitfield packet to send");

        BitFieldPacket packet = new BitFieldPacket();
        BitSet bitset = PieceStatus.piecesToBitset(PeerProcess.localPeerManager.getLocalPieces());
        
        // We can always send the bitfield, even if empty, so this code isn't necessary
        // if (bitset.isEmpty())
        // {
        //     // we have no pieces, so we dont send a bitfield packet
        //     System.out.println("[HANDLER (" + this.state.getRemotePeerId() + ")] Not sending Bitfield, as we have no pieces");
        //     return;
        // }
        
        packet.setData(bitset);

        try {
            this.outgoingMessageQueue.put(packet);
        } catch (InterruptedException exception) {
            System.out.println("[HANDLER (" + this.state.getRemotePeerId() + ")] Failed to prepare Bitfield packet to send");
        }
    }

    private void sendInterested() {
        System.out.println("[HANDLER (" + this.state.getRemotePeerId() + ")] Preparing Interested packet to send");

        Packet packet = new InterestedPacket();

        try {
            this.outgoingMessageQueue.put(packet);
        } catch (InterruptedException exception) {
            System.out.println("[HANDLER (" + this.state.getRemotePeerId() + ")] Failed to prepare Interested packet to send");
        }
    }

    private void sendNotInterested() {
        System.out.println("[HANDLER (" + this.state.getRemotePeerId() + ")] Preparing Not Interested packet to send");

        Packet packet = new NotInterestedPacket();

        try {
            this.outgoingMessageQueue.put(packet);
        } catch (InterruptedException exception) {
            System.out.println("[HANDLER (" + this.state.getRemotePeerId() + ")] Failed to prepare Not Interested packet to send");
        }
    }

    private void sendPiece(int pieceIndex, byte[] piece) {
        System.out.println("[HANDLER (" + this.state.getRemotePeerId() + ")] Preparing Piece packet to send (piece index: " + pieceIndex + ")");

        PiecePacket packet = new PiecePacket();
        packet.setData(pieceIndex, piece);

        try {
            this.outgoingMessageQueue.put(packet);
        } catch (InterruptedException exception) {
            System.out.println("[HANDLER (" + this.state.getRemotePeerId() + ")] Failed to prepare Piece packet to send");
        }
    }

    public void sendHave(int pieceIndex) {
        if (!bitfieldSent)
        {
            System.out.println("[HANDLER (" + this.state.getRemotePeerId()
                    + ")] Not sending have packet as bitfield hasn't been sent yet");
            return;
        }   
        System.out.println("[HANDLER (" + this.state.getRemotePeerId() + ")] Preparing Have packet to send");

        HavePacket packet = new HavePacket();
        packet.setData(pieceIndex);

        try {
            this.outgoingMessageQueue.put(packet);
        } catch (InterruptedException exception) {
            System.out.println("[HANDLER (" + this.state.getRemotePeerId() + ")] Failed to prepare Have packet to send");
        }
    }

    public void sendChoke() {
        System.out.println("[HANDLER (" + this.state.getRemotePeerId() + ")] Preparing Choke packet to send");

        ChokePacket packet = new ChokePacket();

        try {
            this.outgoingMessageQueue.put(packet);
        } catch (InterruptedException exception) {
            System.out.println("[HANDLER (" + this.state.getRemotePeerId() + ")] Failed to prepare Choke packet to send");
        }
    }

    public void sendUnchoke() {
        System.out.println("[HANDLER (" + this.state.getRemotePeerId() + ")] Preparing Unchoke packet to send");

        UnchokePacket packet = new UnchokePacket();

        try {
            this.outgoingMessageQueue.put(packet);
        } catch (InterruptedException exception) {
            System.out.println("[HANDLER (" + this.state.getRemotePeerId() + ")] Failed to prepare Unchoke packet to send");
        }
    }

    private void sendRequest() {
        int desiredPieceIndex = PeerProcess.localPeerManager.choosePiece(this.state.getPieces());

        if(desiredPieceIndex != -1) {
            System.out.println("[HANDLER (" + this.state.getRemotePeerId() + ")] Preparing Request packet to send (requesting piece " + desiredPieceIndex + ")");

            RequestPacket packet = new RequestPacket();
            packet.setData(desiredPieceIndex);

            try {
                this.outgoingMessageQueue.put(packet);
            } catch (InterruptedException exception) {
                System.out.println("[HANDLER (" + this.state.getRemotePeerId() + ")] Failed to prepare Request packet to send");
            }
        }
    }


    private void dumpFile() {
        // TODO: move this function to local peer manager
        String filePath =  "peer_" + this.state.getLocalPeerId() + File.separator + PeerProcess.config.getFileName();

        File file = new File((new File(filePath)).getAbsolutePath());

        try (FileOutputStream fos = new FileOutputStream(file)) {
            if (!file.exists()) {
                file.createNewFile(); // Create the file if it doesn't exist
            }

            for (Piece piece : PeerProcess.localPeerManager.getLocalPieces()) {
                fos.write(piece.getContent()); // Write each byte array to the file
            }

            System.out.println("[FILE DUMPER] Dumped all content into the file");
        } catch (IOException e) {
            System.err.println("[FILE DUMPER] Attempting to dump the content into the file has failed");
        }
    }


    public void terminate() {
        System.out.println("[MANAGER] Setting connection to not active");
        this.state.setConnectionActive(false);

        try {
            this.connection.close();
        } catch(IOException exception) {
            exception.printStackTrace();
        }
    }
}
