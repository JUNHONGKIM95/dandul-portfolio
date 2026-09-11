package com.junhong.somin.admin;

import com.junhong.somin.admin.AdminDtos.AdminMetricsResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminMetricsController {

	private final AdminMetricsService service;

	public AdminMetricsController(AdminMetricsService service) {
		this.service = service;
	}

	@GetMapping("/metrics")
	public AdminMetricsResponse metrics(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
		return service.metrics(startDate, endDate);
	}
}
