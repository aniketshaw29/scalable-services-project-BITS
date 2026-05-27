package com.campuseventhub.registration.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient(name = "event-service", fallback = EventClientFallback.class)
public interface EventClient {

    @GetMapping("/api/events/{id}")
    EventDto getEventById(@PathVariable Long id);

    @PutMapping("/api/events/{id}/capacity")
    EventDto updateCapacity(@PathVariable Long id, @RequestBody Map<String, Integer> request);
}
