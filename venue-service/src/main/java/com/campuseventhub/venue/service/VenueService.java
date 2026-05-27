package com.campuseventhub.venue.service;

import com.campuseventhub.venue.dto.*;
import com.campuseventhub.venue.entity.*;
import com.campuseventhub.venue.exception.*;
import com.campuseventhub.venue.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VenueService {

    private final VenueRepository venueRepository;
    private final VenueBookingRepository bookingRepository;

    @Transactional
    public VenueResponse createVenue(VenueRequest request) {
        Venue venue = Venue.builder()
                .name(request.getName())
                .location(request.getLocation())
                .capacity(request.getCapacity())
                .type(request.getType())
                .facilities(request.getFacilities())
                .build();
        return toVenueResponse(venueRepository.save(venue));
    }

    public List<VenueResponse> getAllVenues() {
        return venueRepository.findAll().stream().map(this::toVenueResponse).toList();
    }

    public VenueResponse getVenueById(Long id) {
        return toVenueResponse(findVenueOrThrow(id));
    }

    public AvailabilityResponse checkAvailability(Long venueId, LocalDateTime startTime, LocalDateTime endTime) {
        findVenueOrThrow(venueId);
        List<VenueBooking> conflicts = bookingRepository.findConflicts(venueId, startTime, endTime);
        if (conflicts.isEmpty()) {
            return AvailabilityResponse.builder().available(true).build();
        }
        return AvailabilityResponse.builder()
                .available(false)
                .conflict(toBookingResponse(conflicts.get(0)))
                .build();
    }

    @Transactional
    public BookingResponse bookVenue(Long venueId, BookingRequest request) {
        findVenueOrThrow(venueId);
        List<VenueBooking> conflicts = bookingRepository.findConflicts(venueId, request.getStartTime(), request.getEndTime());
        if (!conflicts.isEmpty()) {
            throw new VenueBookingConflictException(venueId);
        }
        VenueBooking booking = VenueBooking.builder()
                .venueId(venueId)
                .eventId(request.getEventId())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(BookingStatus.BOOKED)
                .build();
        return toBookingResponse(bookingRepository.save(booking));
    }

    @Transactional
    public void cancelBooking(Long bookingId) {
        VenueBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new VenueNotFoundException(bookingId));
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    public VenueResponse getVenueByEventId(Long eventId) {
        VenueBooking booking = bookingRepository.findByEventIdAndStatus(eventId, BookingStatus.BOOKED)
                .orElseThrow(() -> new VenueNotFoundException(eventId));
        return toVenueResponse(findVenueOrThrow(booking.getVenueId()));
    }

    private Venue findVenueOrThrow(Long id) {
        return venueRepository.findById(id)
                .orElseThrow(() -> new VenueNotFoundException(id));
    }

    private VenueResponse toVenueResponse(Venue v) {
        return VenueResponse.builder()
                .id(v.getId())
                .name(v.getName())
                .location(v.getLocation())
                .capacity(v.getCapacity())
                .type(v.getType())
                .facilities(v.getFacilities())
                .createdAt(v.getCreatedAt())
                .build();
    }

    private BookingResponse toBookingResponse(VenueBooking b) {
        return BookingResponse.builder()
                .bookingId(b.getId())
                .venueId(b.getVenueId())
                .eventId(b.getEventId())
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .status(b.getStatus())
                .build();
    }
}
