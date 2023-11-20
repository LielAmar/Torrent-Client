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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LocalPeerManager {

    private static final Random random = new Random();
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final int localId;

    private final Piece[] localPieces;
    private final Lock choosePieceLock; // TODO: might wanna redo this code

    /*
     * I noticed some potential problems in how the bitmap packet is generated and sent,
     * and how the have packets are communicated internally between threads.
     * EX: Communication with 2 peers, A and B
     * IF the connection with A is set up well before the connection to B, it is possible that we are exchanging data
     * with peer A before we have sent/recieved the handshake with B. If this happened, A might recieve a piece and
     * when anouncing it, adds the have message to connection B's queue before the handshake.
     * This would mean that the handshake isn't the first thing sent to B and the connection would fail.
     * Also, if B has already gotten the bitmap from the LocalPeerManager
     * but hasn't yet enabled listining to other connections "have" messages, then it could end up with 
     * Peer B having a incorrect view of what pieces we have.
     * 
     * Solution: this ReadWriteLock 'bitMapLock'
     * When a connetion is preparing to send its bitmap, we want to stop all other threads from submiting 
     * the pieces they have recieved until the new connection is in a stable state.
     * We are actually somewhat misusing this lock.
     * When a thread recieves a piece it aquires the reader lock before marking it as had in the LocalPeerManager
     * and anouncing the change in state to other threads. 
     * The connection reading to generate its bitmap to send actually aquires the writer lock,
     * so that no other threads can mark and piece as had until this thread is done setting up its bitmap.
     * 
     */
    private final ReadWriteLock bitMapLock;

    private final ArrayList<PeerConnectionManager> connectedPeers;

    private PeerConnectionManager optimisticallyUnchoked;

    public LocalPeerManager(int localId, int numberOfPieces) {
        this.localId = localId;

        this.localPieces = new Piece[numberOfPieces];
        this.choosePieceLock = new ReentrantLock();

        this.bitMapLock = new ReentrantReadWriteLock();

        this.connectedPeers = new ArrayList<>();

        this.optimisticallyUnchoked = null;

        executor.scheduleAtFixedRate(this::setUnchoked, 0, PeerProcess.config.getUnchokingInterval(), TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(this::setOptimalUnchoked, 0, PeerProcess.config.getOptimisticUnchokingInterval(), TimeUnit.SECONDS);
    }

    public void AquireBitMapLock()
    {
        this.bitMapLock.writeLock().lock();
    }

    public void ReleaseBitMapLock()
    {
        this.bitMapLock.writeLock().unlock();
    }

    public Piece[] getLocalPieces() {
        return this.localPieces;
    }


    public void setLocalPiece(int pieceId, PieceStatus status, byte[] content) {
        this.bitMapLock.readLock().lock();
        System.out.println("[LOCAL PEER] Setting piece " + pieceId + " to status " + status.name() + ((content == null) ? " without " : " with " + "content"));

        if(this.localPieces[pieceId] == null) {
            this.localPieces[pieceId] = new Piece(status, content);
        } else {
            this.localPieces[pieceId].setStatus(status);
            this.localPieces[pieceId].setContent(content);
        }
        announce(pieceId);
        this.bitMapLock.readLock().unlock();
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
        // int randomIndex = -1;

        ArrayList<Integer> desired = new ArrayList<>();

        for (int i = 0; i < remotePieces.length; i++) {
            if (remotePieces[i] == PieceStatus.HAVE && this.localPieces[i].getStatus() == PieceStatus.NOT_HAVE) {
                desired.add(i);
            }
        }

        int randomIndex = -1;

        if (desired.size() != 0) {
            randomIndex = desired.get(random.nextInt(desired.size()));
            this.localPieces[randomIndex].setStatus(PieceStatus.REQUESTED);
        }

        // for (int i = 0; i < remotePieces.length; i++) {
        //     if (remotePieces[i] == PieceStatus.HAVE && this.localPieces[i].getStatus() == PieceStatus.NOT_HAVE) {
        //         this.localPieces[i].setStatus(PieceStatus.REQUESTED);
        //         this.choosePieceLock.unlock();
        //         return i;
        //     }
        // }

        this.choosePieceLock.unlock();
        return randomIndex;
    }

    // this is now private, and is only called in setLocalPiece
    private void announce(int pieceIndex) {
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

        System.out.println("=======");
        unchoked.forEach(peer -> {
            System.out.println("Unchoking peer " + peer.getConnectionState().getRemotePeerId());
            if(peer.getConnectionState().isLocalChoked()) {
                peer.getConnectionState().setLocalChoked(false);
                peer.sendUnchoke();
            }
        });

        choked.forEach(peer -> {
            System.out.println("Choking peer " + peer.getConnectionState().getRemotePeerId());
            if(!peer.getConnectionState().isLocalChoked()) {
                peer.getConnectionState().setLocalChoked(true);
                peer.sendChoke();
            }
        });
        System.out.println("=======");
    }

    private void setOptimalUnchoked() {
        List<PeerConnectionManager> choked = connectedPeers.stream()
                .filter(peer -> !peer.getConnectionState().isLocalChoked() && peer.getConnectionState().isInterested())
                .toList();

        this.optimisticallyUnchoked = choked.get(random.nextInt(choked.size()));

        if(this.optimisticallyUnchoked.getConnectionState().isLocalChoked()) {
            System.out.println("Unchoking peer optimistically " + this.optimisticallyUnchoked.getConnectionState().getRemotePeerId());

            this.optimisticallyUnchoked.getConnectionState().setLocalChoked(false);
            this.optimisticallyUnchoked.sendUnchoke();
        }
    }



    /**
     * Checks if either the remote peer still has pieces it needs, or the local peer still has pieces it needs.
     *
     * @return   Whether the connection is still needed
     */
    public void checkTerminateConnection(PeerConnectionManager peerConnectionManager) {
        for(int i = 0; i < peerConnectionManager.getConnectionState().getPieces().length; i++) {
            if(peerConnectionManager.getConnectionState().getPieces()[i] != PieceStatus.HAVE) {
                return;
            }
        }

        for(int i = 0; i < this.getLocalPieces().length; i++) {
            if(this.getLocalPieces()[i].getStatus() != PieceStatus.HAVE) {
                return;
            }
        }

        peerConnectionManager.terminate();
        this.connectedPeers.remove(peerConnectionManager);

        if(this.connectedPeers.isEmpty()) {
            executor.close();
        }
    }
}
