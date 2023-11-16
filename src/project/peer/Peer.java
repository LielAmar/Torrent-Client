package project.peer;

import java.util.BitSet;
import java.util.concurrent.CountDownLatch;

public class Peer {

    private final CountDownLatch latch;

    // TODO: Instead of making peer atomic under peerconnection, we can change each parameter here to be atomic
    // that is: use atomic int for peerid, atomic boolean for choked and atomic bit set for bitfield
    private int peerId;

    private boolean choked;
    private BitSet bitfield;

    public Peer(int peerId) {
        this.latch = new CountDownLatch(1);

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

    public CountDownLatch getLatch() {
        return this.latch;
    }

    public BitSet getBitfield() {
        return bitfield;
    }

    public void setBitfield(BitSet bitfield) {
        this.bitfield = bitfield;
    }
}
