package com.afterApp.after.events;

import com.afterApp.after.enums.EventType;

public class PoolPartyEvent implements IEvent{
    public EventType getEventType(){
        return EventType.POOL_PARTY;
    }
}
