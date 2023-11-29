package project.message.InternalMessage.InternalMessages;

import project.message.InternalMessage.InternalMessage;
import project.message.InternalMessage.InternalMessageType;

public class TerminateIntMes extends InternalMessage {
    public TerminateIntMes() {
        super(InternalMessageType.TERMINATE);
    }

}