package com.afterApp.after.events;

import com.afterApp.after.enums.EventType;

public class ChillEvent implements IEvent{
    public EventType getEventType(){
        return EventType.CHILL;
    }
}
