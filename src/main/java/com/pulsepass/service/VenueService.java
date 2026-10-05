package com.pulsepass.service;

import com.pulsepass.dto.response.VenueResponse;

import java.util.List;

/**
 * FR-SVC-001, FR-SVC-002.
 */
public interface VenueService {

    VenueResponse findByCode(String code);

    List<VenueResponse> findActiveVenues();
}
