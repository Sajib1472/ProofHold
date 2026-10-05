package com.proofhold.location;

import com.proofhold.web.NotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/locations")
public class LocationController {

    private final LocationRepository locations;

    public LocationController(LocationRepository locations) {
        this.locations = locations;
    }

    @GetMapping
    public List<LocationResponse> list() {
        return locations.findAll().stream().map(LocationResponse::from).toList();
    }

    @GetMapping("/{locationId}")
    public LocationResponse get(@PathVariable Long locationId) {
        return locations
                .findById(locationId)
                .map(LocationResponse::from)
                .orElseThrow(() -> new NotFoundException("Location " + locationId + " does not exist."));
    }
}
