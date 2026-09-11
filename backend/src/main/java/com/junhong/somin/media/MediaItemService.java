package com.junhong.somin.media;

import com.junhong.somin.event.DateEvent;
import com.junhong.somin.event.DateEventRepository;
import com.junhong.somin.media.MediaDtos.FavoriteRequest;
import com.junhong.somin.media.MediaDtos.MediaResponse;
import com.junhong.somin.media.MediaDtos.RotateRequest;
import com.junhong.somin.media.MediaDtos.UpdateMediaRequest;
import com.junhong.somin.storage.FileStorageService;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class MediaItemService {

	private final MediaItemRepository mediaRepository;
	private final MediaCommentRepository commentRepository;
	private final DateEventRepository eventRepository;
	private final FileStorageService storageService;

	public MediaItemService(
			MediaItemRepository mediaRepository,
			MediaCommentRepository commentRepository,
			DateEventRepository eventRepository,
			FileStorageService storageService) {
		this.mediaRepository = mediaRepository;
		this.commentRepository = commentRepository;
		this.eventRepository = eventRepository;
		this.storageService = storageService;
	}

	@Transactional(readOnly = true)
	public List<MediaResponse> findMedia(Boolean favorite, String eventId) {
		List<MediaItem> items;
		if (Boolean.TRUE.equals(favorite)) {
			items = mediaRepository.findByFavoriteTrueOrderByCreatedAtDesc();
		} else if (eventId != null && !eventId.isBlank()) {
			items = mediaRepository.findByEventIdOrderByCreatedAtDesc(eventId);
		} else {
			items = mediaRepository.findAllByOrderByCreatedAtDesc();
		}
		return toResponses(items);
	}

	public MediaResponse create(
			MultipartFile file,
			MediaType mediaType,
			String eventId,
			String title,
			String memo,
			LocalDate capturedAt,
			String createdBy,
			String creatorNickname,
			int rotationDegrees) {
		validateEvent(eventId);
		String url = storageService.store(
				file,
				mediaType.name().toLowerCase(),
				mediaType == MediaType.PHOTO ? rotationDegrees : 0);
		String safeTitle = title == null || title.isBlank()
				? Objects.requireNonNullElse(file.getOriginalFilename(), "추억")
				: title;
		MediaItem item = new MediaItem(
				blankToNull(eventId),
				mediaType,
				url,
				Objects.requireNonNullElse(file.getOriginalFilename(), "upload"),
				safeTitle,
				memo,
				capturedAt,
				authorId(createdBy),
				authorNickname(creatorNickname));
		return toResponse(mediaRepository.save(item), eventMap(List.of(item)));
	}

	public MediaResponse update(String id, UpdateMediaRequest request) {
		validateEvent(request.eventId());
		MediaItem item = load(id);
		item.setEventId(blankToNull(request.eventId()));
		item.setTitle(request.title());
		item.setMemo(request.memo());
		item.setCapturedAt(request.capturedAt());
		return toResponse(item, eventMap(List.of(item)));
	}

	public MediaResponse updateFavorite(String id, FavoriteRequest request) {
		MediaItem item = load(id);
		item.setFavorite(request.favorite());
		return toResponse(item, eventMap(List.of(item)));
	}

	public MediaResponse rotate(String id, RotateRequest request) {
		MediaItem item = load(id);
		if (item.getMediaType() != MediaType.PHOTO) {
			throw new IllegalArgumentException("Only photos can be rotated.");
		}
		storageService.rotateLocalPublicUrl(item.getUrl(), request.rotationDegrees());
		item.touch();
		return toResponse(item, eventMap(List.of(item)));
	}

	public void delete(String id) {
		commentRepository.deleteByMediaItemId(id);
		mediaRepository.delete(load(id));
	}

	private MediaItem load(String id) {
		return mediaRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Media item not found: " + id));
	}

	private void validateEvent(String eventId) {
		String normalized = blankToNull(eventId);
		if (normalized != null && !eventRepository.existsById(normalized)) {
			throw new EntityNotFoundException("Date event not found: " + normalized);
		}
	}

	private List<MediaResponse> toResponses(List<MediaItem> items) {
		Map<String, DateEvent> events = eventMap(items);
		return items.stream().map(item -> toResponse(item, events)).toList();
	}

	private MediaResponse toResponse(MediaItem item, Map<String, DateEvent> events) {
		DateEvent event = events.get(item.getEventId());
		return new MediaResponse(
				item.getId(),
				item.getEventId(),
				event == null ? null : event.getTitle(),
				event == null ? null : event.getDate(),
				item.getMediaType(),
				item.getUrl(),
				item.getOriginalFileName(),
				item.getTitle(),
				item.getMemo(),
				item.isFavorite(),
				item.getCapturedAt(),
				authorId(item.getCreatedBy()),
				authorNickname(item.getCreatorNickname()),
				item.getUpdatedAt());
	}

	private Map<String, DateEvent> eventMap(List<MediaItem> items) {
		List<String> eventIds = items.stream()
				.map(MediaItem::getEventId)
				.filter(Objects::nonNull)
				.distinct()
				.toList();
		return eventRepository.findAllById(eventIds).stream()
				.collect(Collectors.toMap(DateEvent::getId, Function.identity()));
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}

	private String authorId(String value) {
		return value == null || value.isBlank() ? "junhong" : value;
	}

	private String authorNickname(String value) {
		return value == null || value.isBlank() ? "준홍" : value;
	}
}
