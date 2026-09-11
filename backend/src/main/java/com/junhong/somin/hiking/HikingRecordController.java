package com.junhong.somin.hiking;

import com.junhong.somin.hiking.HikingDtos.HikingRecordRequest;
import com.junhong.somin.hiking.HikingDtos.HikingRecordResponse;
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
@RequestMapping("/api/hiking-records")
public class HikingRecordController {

	private final HikingRecordService service;

	public HikingRecordController(HikingRecordService service) {
		this.service = service;
	}

	@GetMapping
	public List<HikingRecordResponse> findRecords(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
		return service.findRecords(start, end);
	}

	@PostMapping
	public HikingRecordResponse create(@Valid @RequestBody HikingRecordRequest request) {
		return service.create(request);
	}

	@PutMapping("/{id}")
	public HikingRecordResponse update(@PathVariable String id, @Valid @RequestBody HikingRecordRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	public void delete(@PathVariable String id) {
		service.delete(id);
	}
}
