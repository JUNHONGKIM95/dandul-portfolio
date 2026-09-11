package com.junhong.somin.visit;

import com.junhong.somin.visit.AppVisitDtos.VisitRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/visits")
public class AppVisitController {

	private final AppVisitRepository repository;

	public AppVisitController(AppVisitRepository repository) {
		this.repository = repository;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void create(@Valid @RequestBody VisitRequest request) {
		if ("admin".equals(request.username())) {
			return;
		}
		repository.save(new AppVisit(request.username(), request.nickname()));
	}
}
