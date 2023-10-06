package project.peer;

import java.util.BitSet;

public class Peer {

    // TODO: Instead of making peer atomic under peerconnection, we can change each parameter here to be atomic
    // that is: use atomic int for peerid, atomic boolean for choked and atomic bit set for bitfield
    private int peerId;

    private boolean choked;
    private BitSet bitfield;

    public Peer(int peerId) {
        this.peerId = peerId;

        this.choked = false;
        this.bitfield = null;
    }
    
    public int getPeerId() {
        return this.peerId;
    }

    public void setPeerId(int peerId)
    {
        this.peerId = peerId;
    }
}
