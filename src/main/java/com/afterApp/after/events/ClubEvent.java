package com.afterApp.after.events;

import com.afterApp.after.enums.EventType;

public class ClubEvent implements IEvent{
    public EventType getEventType(){
        return EventType.CLUB;
    }
}
