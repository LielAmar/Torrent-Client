package project;

import project.connection.ConnectionState;
import project.connection.PeerConnectionManager;
import project.connection.piece.Piece;
import project.connection.piece.PieceStatus;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LocalPeerManager {

    private static final Random random = new Random();
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final int localId;

    private final Piece[] localPieces;
    private final Lock choosePieceLock; // TODO: might wanna redo this code

    private final ArrayList<PeerConnectionManager> connectedPeers;

    private PeerConnectionManager optimisticallyUnchoked;

    public LocalPeerManager(int localId, int numberOfPieces) {
        this.localId = localId;

        this.localPieces = new Piece[numberOfPieces];
        this.choosePieceLock = new ReentrantLock();

        this.connectedPeers = new ArrayList<>();

        this.optimisticallyUnchoked = null;

        executor.scheduleAtFixedRate(this::setUnchoked, 0, PeerProcess.config.getUnchokingInterval(), TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(this::setOptimalUnchoked, 0, PeerProcess.config.getOptimisticUnchokingInterval(), TimeUnit.SECONDS);
    }


    public Piece[] getLocalPieces() {
        return this.localPieces;
    }

    public void setLocalPiece(int pieceId, PieceStatus status, byte[] content) {
        System.out.println("[LOCAL PEER] Setting piece to status " + status.name() + ((content == null) ? " without " : " with " + "content"));

        if(this.localPieces[pieceId] == null) {
            this.localPieces[pieceId] = new Piece(status, content);
        } else {
            this.localPieces[pieceId].setStatus(status);
            this.localPieces[pieceId].setContent(content);
        }
    }


    public PeerConnectionManager connectToNewPeer(Socket socket) {
        return this.connectToNewPeer(-1, socket);
    }

    public PeerConnectionManager connectToNewPeer(int remotePeerId, Socket socket) {
        System.out.println("[LOCAL PEER MANAGER] Creating a connection manager with peer " + remotePeerId);

        ConnectionState state = new ConnectionState(this.localId, remotePeerId);
        PeerConnectionManager manager = new PeerConnectionManager(socket, state);
        this.connectedPeers.add(manager);
        return manager;
    }

    public PeerConnectionManager getConnectionManagerById(int peerId) {
        return this.connectedPeers
                .stream()
                .filter(manager -> manager.getConnectionState().getRemotePeerId() == peerId)
                .findFirst()
                .orElse(null);
    }


    public int choosePiece(PieceStatus[] remotePieces) {
        this.choosePieceLock.lock();

        ArrayList<Integer> desired = new ArrayList<>();

        for (int i = 0; i < remotePieces.length; i++) {
            if(remotePieces[i] == PieceStatus.HAVE  && this.localPieces[i].getStatus() == PieceStatus.NOT_HAVE) {
                desired.add(i);
            }
        }

        int randomIndex = -1;

        if(desired.size() != 0) {
            randomIndex = desired.get(random.nextInt(desired.size()));
            this.localPieces[randomIndex].setStatus(PieceStatus.REQUESTED);
        }


        this.choosePieceLock.unlock();
        return randomIndex;
    }

    public void announce(int pieceIndex) {
        for (PeerConnectionManager peerConnection: connectedPeers) {
            peerConnection.sendHave(pieceIndex);
        }
    }

    private void setUnchoked() {
        List<PeerConnectionManager> unchoked = connectedPeers.stream()
                .sorted((pA, pB) -> {
                    if(pA.getConnectionState().getDownloadSpeed() < pB.getConnectionState().getDownloadSpeed()) {
                        return 1;
                    } else if(pA.getConnectionState().getDownloadSpeed() > pB.getConnectionState().getDownloadSpeed()) {
                        return -1;
                    } else {
                        return random.nextInt(2) == 0 ? 1 : -1;
                    }
                })
                .filter(peer -> peer.getConnectionState().isInterested())
                .limit(PeerProcess.config.getNumberOfPreferredNeighbors())
                .toList();

        List<PeerConnectionManager> choked = connectedPeers.stream()
                .filter(peer -> peer != optimisticallyUnchoked && !unchoked.contains(peer))
                .toList();

        unchoked.forEach(peer -> {
            if(peer.getConnectionState().isLocalChoked()) {
                peer.getConnectionState().setLocalChoked(false);
                peer.sendUnchoke();
            }
        });

        choked.forEach(peer -> {
            if(!peer.getConnectionState().isLocalChoked()) {

                peer.getConnectionState().setLocalChoked(true);
                peer.sendChoke();
            }
        });
    }

    private void setOptimalUnchoked() {
        List<PeerConnectionManager> choked = connectedPeers.stream()
                .filter(peer -> !peer.getConnectionState().isLocalChoked() && peer.getConnectionState().isInterested())
                .toList();

        this.optimisticallyUnchoked = choked.get(random.nextInt(choked.size()));

        if(this.optimisticallyUnchoked.getConnectionState().isLocalChoked()) {
            this.optimisticallyUnchoked.getConnectionState().setLocalChoked(false);
            this.optimisticallyUnchoked.sendUnchoke();
        }
    }
}
