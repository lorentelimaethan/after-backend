package com.afterApp.after.events;

import com.afterApp.after.enums.EventType;

public class HousePartyEvent implements IEvent{
    public EventType getEventType(){
        return EventType.HOUSE_PARTY;
    }
}
