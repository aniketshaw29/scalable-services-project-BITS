package com.campuseventhub.registration.client;

import com.campuseventhub.registration.exception.EventServiceUnavailableException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EventClientFallback implements EventClient {

    @Override
    public EventDto getEventById(Long id) {
        throw new EventServiceUnavailableException();
    }

    @Override
    public EventDto updateCapacity(Long id, Map<String, Integer> request) {
        throw new EventServiceUnavailableException();
    }
}
