package project.message;

public abstract class Message {
    private final boolean isPacket;
    public Message(boolean isPacket)
    {
        this.isPacket = isPacket;
    }

    public boolean GetIsPacket()
    {
        return isPacket;
    }

    public boolean GetIsInternal()
    {
        return !isPacket;
    }
    public int compareTo(Message other)
    {
        if(this.GetIsPacket() == other.GetIsPacket())
        {
            // either they are both packets or they are both internal messages
            return 0;
        }
        if (this.GetIsInternal() /* therefore other is a packet */)
        {
            return -1;
        }
        if (this.GetIsPacket() /* threfore other is an internal message */)
        {
            return 1;
        }
        return 0;
    }


}
