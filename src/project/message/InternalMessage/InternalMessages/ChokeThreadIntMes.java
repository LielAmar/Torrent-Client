package project.message.InternalMessage.InternalMessages;

import project.message.InternalMessage.InternalMessage;
import project.message.InternalMessage.InternalMessageType;

public class ChokeThreadIntMes extends InternalMessage {
    public ChokeThreadIntMes() {
        super(InternalMessageType.CHOKE_THREAD);
    }

}
