package project.message.InternalMessage;

import project.message.Message;
import project.message.InternalMessage.InternalMessageType;

public abstract class InternalMessage extends Message {
    protected final InternalMessageType type;

    protected InternalMessage(InternalMessageType type) {
        super(false);
        this.type = type;
    }
    
    public InternalMessageType getType() {
        return this.type;
    }

    public String getTypeString() {
        return type.name();
    }

}
