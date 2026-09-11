package com.junhong.somin.event;

import com.junhong.somin.event.DateEventDtos.EventRequest;
import com.junhong.somin.event.DateEventDtos.EventResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class DateEventController {

	private final DateEventService service;

	public DateEventController(DateEventService service) {
		this.service = service;
	}

	@GetMapping
	public List<EventResponse> findEvents(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
		return service.findEvents(start, end);
	}

	@PostMapping
	public EventResponse create(@Valid @RequestBody EventRequest request) {
		return service.create(request);
	}

	@PutMapping("/{id}")
	public EventResponse update(@PathVariable String id, @Valid @RequestBody EventRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	public void delete(@PathVariable String id) {
		service.delete(id);
	}
}
