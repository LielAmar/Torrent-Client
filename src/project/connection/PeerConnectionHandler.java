package project.connection;

import project.LocalPeerManager;
import project.connection.piece.Piece;
import project.connection.piece.PieceStatus;
import project.message.InternalMessage.InternalMessages.ReceivedIntMes;
import project.message.packet.Packet;
import project.message.packet.packets.*;
import project.utils.Logger;
import project.utils.Tag;

import java.util.BitSet;

public class PeerConnectionHandler {

    private final PeerConnectionManager peerConnectionManager;

    private final LocalPeerManager localPeerManager;
    private final ConnectionState state;

    public PeerConnectionHandler(PeerConnectionManager peerConnectionManager) {
        this.peerConnectionManager = peerConnectionManager;

        this.localPeerManager = peerConnectionManager.getLocalPeerManager();
        this.state = peerConnectionManager.getConnectionState();
    }

    /**
     * Handles a single received packet by parsing it, updating local state and preparing a reply packet
     *
     * @param packet   Packet to handle
     */
    public void handle(Packet packet) {
        Logger.print(Tag.HANDLER, "Received a " + packet.getTypeString() + " from peer " +
                this.state.getRemotePeerId());

        switch (packet.getType()) {
            case CHOKE:
                handleChoke((ChokePacket) packet);
                break;
            case UNCHOKE:
                handleUnchoke((UnchokePacket) packet);
                break;

            case INTERESTED:
                handleInterested((InterestedPacket) packet);
                break;
            case NOT_INTERESTED:
                handleNotInterested((NotInterestedPacket) packet);
                break;

            case BITFIELD:
                handleBitfield((BitFieldPacket) packet);
                break;

            case HAVE:
                handleHave((HavePacket) packet);
                break;

            case REQUEST:
                handleRequest((RequestPacket) packet);
                break;
            case PIECE:
                handlePiece((PiecePacket) packet);
                break;

            default:
                break;
        }
    }

    /**
     * handles receiving a Choke packet.
     * According to the protocol, when local peer is being choked by remote peer, the local peer
     * is not able to request pieces from remote peer.
     * Thus, when receiving an Unchoke packet, we update the remote choke value
     *
     * @param packet   The Unchoke Packet
     */
    private void handleChoke(ChokePacket packet) {
        this.localPeerManager.getLogger().log("Peer " + this.localPeerManager.getLocalPeerId() +
                " is choked by " + this.state.getRemotePeerId() + ".");

        if (this.state.isRemoteChoked())
        {
            System.out.println("Peer " + this.localPeerManager.getLocalPeerId() + " was choked by "
                    + this.state.getRemotePeerId() + " even though it was already choked");
            System.out.flush();
            this.localPeerManager.getLogger().close();
            System.exit(0);
        }
        
        this.state.setRemoteChoke(true);

        if(this.state.getPieceRequested())
        {
            this.localPeerManager.cancelPieceRequest(this.state.getPieceRequestedID());
        }
        this.state.setPieceRequested(false);
    }

    /**
     * handles receiving an Unchoke packet.
     * According to the protocol, when local peer is being unchoked by remote peer, the local peer
     * is able to request pieces from remote peer.
     * Thus, when receiving an Unchoke packet, we update the remote choke value and the local peer
     * sends a Request packet back.
     *
     * @param packet   The Unchoke Packet
     */
    private void handleUnchoke(UnchokePacket packet) {
        this.localPeerManager.getLogger().log("Peer " + this.localPeerManager.getLocalPeerId() +
                " is unchoked by " + this.state.getRemotePeerId() + ".");

        if (!this.state.isRemoteChoked())
        {
            System.out.println("Peer " + this.localPeerManager.getLocalPeerId() +
                    " was unchoked by " + this.state.getRemotePeerId() + " even though it was already unchoked");
                System.out.flush();
            this.localPeerManager.getLogger().close();
            System.exit(0);
        }
        this.state.setRemoteChoke(false);

        // handle the edge case where at the start, we sent a request before we were ever unchoked
        // and then we recieve an unchoke without ever recieveing a choke that would cancel the original request
        if (this.state.getPieceRequested())
        {
            sendRequest(this.state.getPieceRequestedID());
        }
        else
        {
            this.SetInterestAndRequest();
        }
    }

    /**
     * handles receiving an Interested packet.
     * According to the protocol, when local peer is getting an Interested packet from the remote peer, the local peer
     * knows it has pieces the remote peer wants.
     * Thus, when receiving an Interested packet, local peer updates the connection state to Interested=true
     *
     * @param packet   The Interested Packet
     */
    private void handleInterested(InterestedPacket packet) {
        this.localPeerManager.getLogger().log("Peer " + this.localPeerManager.getLocalPeerId() +
                " received the ‘interested’ message from " + this.state.getRemotePeerId() + ".");

        this.state.setInterested(true);
    }

    /**
     * handles receiving a NotInterested packet.
     * According to the protocol, when local peer is getting a NotInterested packet from the remote peer, the local peer
     * knows it has no pieces the remote peer wants.
     * Thus, when receiving a NotInterested packet, local peer updates the connection state to Interested=false
     *
     * @param packet   The NotInterested Packet
     */
    private void handleNotInterested(NotInterestedPacket packet) {
        this.localPeerManager.getLogger().log("Peer " + this.localPeerManager.getLocalPeerId() +
                " received the ‘not interested’ message from " + this.state.getRemotePeerId() + ".");

        this.state.setInterested(false);
    }

    /**
     * handles receiving a Bitfield packet.
     * According to the protocol, when local peer is getting a Bitfield packet from the remote peer, the local peer
     * knows the remote peer's bitfield status.
     * Thus, when receiving a Bitfield packet, local peer updates the connection state with the remote peer's pieces.
     * In addition, if the local peer has any interest in the remote peer's pieces, it sends an Interested
     * packet, and otherwise, sends a NotInterested packet.
     *
     * @param packet   The Bitfield Packet
     */
    private void handleBitfield(BitFieldPacket packet) {
        PieceStatus[] pieces = PieceStatus.bitsetToPiecesStatus(packet.getBitfield(),
                this.localPeerManager.getConfig().getNumberOfPieces());

        this.state.setPieces(pieces);

        // if(this.hasInterest()) {
        //     this.sendInterested();
        //     this.state.setLocalInterestedIn(true);
        // } else {
        //     this.sendNotInterested();
        //     this.state.setLocalInterestedIn(false);
        // }
        this.SetInterestAndRequest();
    }

    /**
     * handles receiving a Have packet.
     * According to the protocol, when local peer is getting a Have packet from the remote peer, the local peer
     * knows the remote peer's status for a specific piece.
     * Thus, when receiving a Have packet, local peer updates the connection state with the remote peer's piece status.
     * In addition, if the local peer has any interest in the updated remote peer's pieces, it sends an Interested
     * packet, and otherwise, sends a NotInterested packet.
     *
     * @param packet   The Have Packet
     */
    private void handleHave(HavePacket packet) {
        this.localPeerManager.getLogger().log("Peer " + this.localPeerManager.getLocalPeerId() +
                " received the ‘have’ message from " + this.state.getRemotePeerId() + " for the piece "
                + packet.getPieceIndex() + ".");

        int pieceIndex = packet.getPieceIndex();

        this.state.updatePiece(pieceIndex);

        //         if(this.hasInterest()) {
        //             this.sendInterested();

        //             if(!this.state.isLocalInterestedIn()) {
        //                 // TODO: sometimes, peer B doesn't send peer C any pieces. Try to fix it.
        // //                if(!this.state.isRemoteChoked()) {
        // //                    // If there's more pieces to request, request one.
        // //                    pieceIndex = this.localPeerManager.choosePieceToRequest(this.state.getPieces());
        // //
        // //                    if(pieceIndex != -1 && !this.state.isRemoteChoked()) {
        // //                        this.sendRequest(pieceIndex);
        // //                    }
        // //                }

        //                 this.state.setLocalInterestedIn(true);
        //             }
        //         } else {
        //             this.sendNotInterested();

        //             this.state.setLocalInterestedIn(false);
        //         }

        this.SetInterestAndRequest();

        // Check if the connection needs to be terminated (both local & remote peers have all pieces)
        // this.localPeerManager.attemptTerminate(this.peerConnectionManager);
    }

    /**
     * handles receiving a Request packet.
     * According to the protocol, when local peer is getting a Request packet from the remote peer, the local peer
     * knows the remote peer wants a specific piece it has.
     * Thus, when receiving a Request packet, the local peer makes sure that the remote peer is not choked
     * locally (meaning that the local peer choked remote peer <=> local peer refuses sending data to remote), and
     * sends a Piece packet back.
     *
     * @param packet   The Request Packet
     */
    private void handleRequest(RequestPacket packet) {
        int pieceIndex = packet.getPieceIndex();

        // If the remote peer is not locally choked, it means the local peer can send pieces to it.
        if(!this.state.isLocalChoked()) {
            this.sendPiece(pieceIndex);
        }
    }

    /**
     * handles receiving a Piece packet.
     * According to the protocol, when local peer is getting a Piece packet from the remote peer, the local peer
     * now has a specific piece content.
     * Thus, when receiving a Piece packet, the local peer updates the piece's content locally and then checks
     * if there are any other pieces it wants from the remote peer. If there are, a Request packet is being sent,
     * and otherwise, checks if all pieces were received, in which case, the file is being dumped.
     *
     * @param packet   The Piece Packet
     */
    private void handlePiece(PiecePacket packet) {
        this.localPeerManager.getLogger().log("Peer " + this.localPeerManager.getLocalPeerId() +
                " has downloaded the piece " + packet.getPieceIndex() + " from " + this.state.getRemotePeerId() + "." +
                " Now the number of pieces it has is " + this.localPeerManager.getLocalPiecesCount() + ".");

        // this.localPeerManager.setLocalPiece(packet.getPieceIndex(), PieceStatus.HAVE, packet.getPieceContent());
        this.localPeerManager.SendControlMessage(new ReceivedIntMes(packet.getPieceIndex(), packet.getPieceContent()));
        this.state.increaseDownloadSpeed();

        // we no longer have a requested piece, as we have recieved the previous one and havent requested another yet
        this.state.setPieceRequested(false);
        this.SetInterestAndRequest();

        // Check if the connection needs to be terminated (both local & remote peers have all pieces)
        // this.localPeerManager.attemptTerminate();
    }


    public void sendBitfield() {
        Logger.print(Tag.HANDLER, "Preparing a Bitfield packet to send to peer " +
                this.state.getRemotePeerId());

        BitFieldPacket packet = new BitFieldPacket();
        BitSet bitset = PieceStatus.piecesToBitset(this.localPeerManager.getLocalPieces());

        packet.setData(bitset);

        try {
            this.peerConnectionManager.preparePacket(packet);

            this.state.setSentBitfield(true);
        } catch (InterruptedException exception) {
            System.err.println("An error occurred when trying to send a Bitfield packet to peer " +
                    this.state.getRemotePeerId());
        }
    }

    public void sendInterested() {
        Logger.print(Tag.HANDLER, "Preparing an Interested packet to send to peer " +
                this.state.getRemotePeerId());

        Packet packet = new InterestedPacket();

        try {
            this.peerConnectionManager.preparePacket(packet);
        } catch (InterruptedException exception) {
            System.err.println("An error occurred when trying to send an Interested packet to peer " +
                    this.state.getRemotePeerId());
        }
    }

    public void sendNotInterested() {
        Logger.print(Tag.HANDLER, "Preparing a NotInterested packet to send to peer " +
                this.state.getRemotePeerId());

        Packet packet = new NotInterestedPacket();

        try {
            this.peerConnectionManager.preparePacket(packet);
        } catch (InterruptedException exception) {
            System.err.println("An error occurred when trying to send a NotInterested packet to peer " +
                    this.state.getRemotePeerId());
        }
    }

    public void sendPiece(int pieceIndex) {
        Logger.print(Tag.HANDLER, "Preparing a Piece packet to send to peer " +
                this.state.getRemotePeerId());

        if(this.localPeerManager.getLocalPieces()[pieceIndex].getContent() == null) {
            Logger.print(Tag.DEBUG, "Tried to send a PIECE packet with a piece " + pieceIndex + " local peer doesn't have to peer " +
                    this.state.getRemotePeerId());
            return;
        }

        PiecePacket packet = new PiecePacket();
        packet.setData(pieceIndex, this.localPeerManager.getLocalPieces()[pieceIndex].getContent());

        try {
            this.peerConnectionManager.preparePacket(packet);
        } catch (InterruptedException exception) {
            System.err.println("An error occurred when trying to send a Piece packet to peer " +
                    this.state.getRemotePeerId());
        }
    }

    public void sendHave(int pieceIndex) {
        Logger.print(Tag.HANDLER, "Preparing a Have packet to send to peer " +
                this.state.getRemotePeerId());

        if (!this.state.hasSentBitfield()) {
            System.err.println("Tried to send a HAVE packet before a BITFIELD packet has been sent");
            return;
        }

        HavePacket packet = new HavePacket();
        packet.setData(pieceIndex);

        try {
            this.peerConnectionManager.preparePacket(packet);
        } catch (InterruptedException exception) {
            System.err.println("An error occurred when trying to send a Piece packet to peer " +
                    this.state.getRemotePeerId());
        }

        // Check if this connection should be terminated and terminate it if so
//        this.localPeerManager.checkTerminateConnection(this);
    }

    public void sendChoke() {
        Logger.print(Tag.HANDLER, "Preparing a Choke packet to send to peer " +
                this.state.getRemotePeerId());

        ChokePacket packet = new ChokePacket();

        try {
            this.peerConnectionManager.preparePacket(packet);
        } catch (InterruptedException exception) {
            System.err.println("An error occurred when trying to send a Choke packet to peer " +
                    this.state.getRemotePeerId());
        }
    }

    public void sendUnchoke() {
        Logger.print(Tag.HANDLER, "Preparing an Unchoke packet to send to peer " +
                this.state.getRemotePeerId());

        UnchokePacket packet = new UnchokePacket();

        try {
            this.peerConnectionManager.preparePacket(packet);
        } catch (InterruptedException exception) {
            System.err.println("An error occurred when trying to send an Unchoke packet to peer " +
                    this.state.getRemotePeerId());
        }
    }

    private void sendRequest(int pieceIndex) {
        Logger.print(Tag.HANDLER, "Preparing a Request packet to send to peer " +
                this.state.getRemotePeerId());

        if (pieceIndex == -1) {
            System.err.println("Tried to send a REQUEST packet with an invalid piece index");
            return;
        }
    
	    if(this.state.getPieces()[pieceIndex] != PieceStatus.HAVE)
        {
            Logger.print(Tag.DEBUG, "Tried to send a REQUEST packet for a piece " + pieceIndex + " that peer " + this.state.getRemotePeerId() + "doesnt have");
            return;
        }

        RequestPacket packet = new RequestPacket();
        packet.setData(pieceIndex);

        try {
            this.peerConnectionManager.preparePacket(packet);
        } catch (InterruptedException exception) {
            System.err.println("An error occurred when trying to send an Request packet to peer " +
                    this.state.getRemotePeerId());
        }
    }


    /**
     * Checks if the local peer has any interest in the remote peer
     *
     * @return   Whether the remote peer has any pieces the local peer wants
     */
    public boolean hasInterest() {
        Piece[] local = this.localPeerManager.getLocalPieces();
        PieceStatus[] remote = this.state.getPieces();

        for (int pieceId = 0; pieceId < Math.min(local.length, remote.length); pieceId++) {
            if (local[pieceId].getStatus() != PieceStatus.HAVE && remote[pieceId] == PieceStatus.HAVE) {
                return true;
            }
        }

        return false;
    }
    
    public void SetInterestAndRequest()
    {
        // System.out.println("PeerConnectionHandler " + this.state.getRemotePeerId() + " updating interest and requests");
        Logger.print(Tag.HANDLER, "PeerConnectionHandler " + this.state.getRemotePeerId() + " updating interest and requests");

        // // If there's more pieces to request, request one.
        // int pieceIndex = this.localPeerManager.choosePieceToRequest(this.state.getPieces());

        // if(pieceIndex != -1 && !this.state.isRemoteChoked()) {
        //     this.sendRequest(pieceIndex);
        //     return;
        // }

        boolean shouldBeInterested = hasInterest();
        if (!shouldBeInterested && this.state.isLocalInterestedIn())
        {
            Logger.print(Tag.DEBUG,  "PeerConnectionHandler " + this.state.getRemotePeerId() + "changing interest from interested to not");
        }
        if(shouldBeInterested != this.state.isLocalInterestedIn())
        {
            // System.out.println("PeerConnectionHandler " + this.state.getRemotePeerId() + " had the incorect interest state, changing from " + (shouldBeInterested ? "Uninterested" : "Interested") + " to " + (shouldBeInterested ? "Interested" : "Uninterested"));
            Logger.print(Tag.DEBUG, "PeerConnectionHandler " + this.state.getRemotePeerId() + " had the incorect interest state, changing from " + (shouldBeInterested ? "Uninterested" : "Interested") + " to " + (shouldBeInterested ? "Interested" : "Uninterested"));

            // if we aren't already in the correct state for some reason
            // (they recieved a new piece or we recieved a piece and we no longer need anything from them)
            // send the appropriate packet and update our state
            if (shouldBeInterested) {
                sendInterested();
            } else {
                sendNotInterested();
            }
            this.state.setLocalInterestedIn(shouldBeInterested);
        }
        
        if(this.state.isLocalInterestedIn() && !this.state.isRemoteChoked())
        {
            if(!this.state.getPieceRequested())
            {
                int pieceID = this.localPeerManager.choosePieceToRequest(this.state.getPieces());
                // System.out.println("PeerConnectionHandler " + this.state.getRemotePeerId() + " didn't have a piece requested, now requesting " + pieceID);
                Logger.print(Tag.DEBUG, "PeerConnectionHandler " + this.state.getRemotePeerId() + " didn't have a piece requested, now requesting " + pieceID);

                if (pieceID >= 0)
                {
                    sendRequest(pieceID);
                    this.state.setPieceRequested(true);
                    this.state.setPieceRequestedID(pieceID);
                }
                
            }
        }
    }
}
