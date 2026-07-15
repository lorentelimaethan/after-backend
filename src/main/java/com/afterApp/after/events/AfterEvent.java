package com.afterApp.after.events;

import com.afterApp.after.enums.EventType;

public class AfterEvent implements IEvent{
    public EventType getEventType(){
        return EventType.AFTER;
    }
}
