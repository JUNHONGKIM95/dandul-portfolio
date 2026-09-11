package com.junhong.somin.event;

import com.junhong.somin.event.DateEventDtos.EventRequest;
import com.junhong.somin.event.DateEventDtos.EventResponse;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DateEventService {

	private final DateEventRepository repository;

	public DateEventService(DateEventRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public List<EventResponse> findEvents(LocalDate start, LocalDate end) {
		List<DateEvent> events = start != null && end != null
				? repository.findOverlappingRangeOrderByDateAscMeetingTimeAsc(start, end)
				: repository.findAllByOrderByDateAscMeetingTimeAsc();
		return events.stream().map(this::toResponse).toList();
	}

	public EventResponse create(EventRequest request) {
		DateEvent event = new DateEvent(
				request.date(),
				normalizedEndDate(request.date(), request.endDate()),
				request.meetingTime(),
				request.place(),
				request.title(),
				request.memo(),
				category(request.category()),
				authorId(request.createdBy()),
				authorNickname(request.creatorNickname()));
		return toResponse(repository.save(event));
	}

	public EventResponse update(String id, EventRequest request) {
		DateEvent event = load(id);
		event.setDate(request.date());
		event.setEndDate(normalizedEndDate(request.date(), request.endDate()));
		event.setMeetingTime(request.meetingTime());
		event.setPlace(request.place());
		event.setTitle(request.title());
		event.setMemo(request.memo());
		event.setCategory(category(request.category()));
		event.setCreatedBy(authorId(request.createdBy()));
		event.setCreatorNickname(authorNickname(request.creatorNickname()));
		return toResponse(event);
	}

	public void delete(String id) {
		repository.delete(load(id));
	}

	private DateEvent load(String id) {
		return repository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Date event not found: " + id));
	}

	private EventResponse toResponse(DateEvent event) {
		return new EventResponse(
				event.getId(),
				event.getDate(),
				normalizedEndDate(event.getDate(), event.getEndDate()),
				event.getMeetingTime(),
				event.getPlace(),
				event.getTitle(),
				event.getMemo(),
				category(event.getCategory()),
				authorId(event.getCreatedBy()),
				authorNickname(event.getCreatorNickname()));
	}

	private String category(String value) {
		if ("junhong".equals(value) || "somin".equals(value) || "together".equals(value)) {
			return value;
		}
		return "together";
	}

	private LocalDate normalizedEndDate(LocalDate startDate, LocalDate endDate) {
		if (endDate == null || endDate.isBefore(startDate)) {
			return startDate;
		}
		return endDate;
	}

	private String authorId(String value) {
		return value == null || value.isBlank() ? "junhong" : value;
	}

	private String authorNickname(String value) {
		return value == null || value.isBlank() ? "준홍" : value;
	}
}
