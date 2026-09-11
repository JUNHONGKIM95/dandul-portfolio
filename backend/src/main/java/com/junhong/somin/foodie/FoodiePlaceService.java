package com.junhong.somin.foodie;

import com.junhong.somin.event.DateEvent;
import com.junhong.somin.event.DateEventRepository;
import com.junhong.somin.foodie.FoodieDtos.FoodiePlaceResponse;
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
public class FoodiePlaceService {

	private final FoodiePlaceRepository foodieRepository;
	private final DateEventRepository eventRepository;
	private final FileStorageService storageService;

	public FoodiePlaceService(
			FoodiePlaceRepository foodieRepository,
			DateEventRepository eventRepository,
			FileStorageService storageService) {
		this.foodieRepository = foodieRepository;
		this.eventRepository = eventRepository;
		this.storageService = storageService;
	}

	@Transactional(readOnly = true)
	public List<FoodiePlaceResponse> findPlaces() {
		List<FoodiePlace> places = foodieRepository.findAllByOrderByVisitDateDescUpdatedAtDesc();
		return toResponses(places);
	}

	public FoodiePlaceResponse create(
			MultipartFile photo,
			String kakaoPlaceId,
			String name,
			String categoryName,
			String addressName,
			String roadAddressName,
			String phone,
			String placeUrl,
			Double latitude,
			Double longitude,
			Double kakaoRating,
			Integer kakaoReviewCount,
			String eventId,
			LocalDate visitDate,
			String oneLineReview,
			Double junhongRating,
			Double sominRating,
			String junhongReview,
			String sominReview,
			String createdBy,
			String creatorNickname) {
		validateEvent(eventId);
		String photoUrl = photo == null || photo.isEmpty() ? null : storageService.store(photo, "foodie");
		FoodiePlace place = new FoodiePlace(
				blankToNull(kakaoPlaceId),
				name,
				blankToNull(categoryName),
				blankToNull(addressName),
				blankToNull(roadAddressName),
				blankToNull(phone),
				blankToNull(placeUrl),
				latitude,
				longitude,
				kakaoRating,
				kakaoReviewCount,
				blankToNull(eventId),
				visitDate,
				photoUrl,
				blankToNull(oneLineReview),
				junhongRating,
				sominRating,
				blankToNull(junhongReview),
				blankToNull(sominReview),
				authorId(createdBy),
				authorNickname(creatorNickname));
		return toResponse(foodieRepository.save(place), eventMap(List.of(place)));
	}

	public FoodiePlaceResponse update(
			String id,
			MultipartFile photo,
			String kakaoPlaceId,
			String name,
			String categoryName,
			String addressName,
			String roadAddressName,
			String phone,
			String placeUrl,
			Double latitude,
			Double longitude,
			Double kakaoRating,
			Integer kakaoReviewCount,
			String eventId,
			LocalDate visitDate,
			String oneLineReview,
			Double junhongRating,
			Double sominRating,
			String junhongReview,
			String sominReview,
			String createdBy,
			String creatorNickname) {
		validateEvent(eventId);
		FoodiePlace place = load(id);
		place.setKakaoPlaceId(blankToNull(kakaoPlaceId));
		place.setName(name);
		place.setCategoryName(blankToNull(categoryName));
		place.setAddressName(blankToNull(addressName));
		place.setRoadAddressName(blankToNull(roadAddressName));
		place.setPhone(blankToNull(phone));
		place.setPlaceUrl(blankToNull(placeUrl));
		place.setLatitude(latitude);
		place.setLongitude(longitude);
		place.setKakaoRating(kakaoRating);
		place.setKakaoReviewCount(kakaoReviewCount);
		place.setEventId(blankToNull(eventId));
		place.setVisitDate(visitDate);
		if (photo != null && !photo.isEmpty()) {
			place.setPhotoUrl(storageService.store(photo, "foodie"));
		}
		place.setOneLineReview(blankToNull(oneLineReview));
		place.setJunhongRating(junhongRating);
		place.setSominRating(sominRating);
		place.setJunhongReview(blankToNull(junhongReview));
		place.setSominReview(blankToNull(sominReview));
		place.setCreatedBy(authorId(createdBy));
		place.setCreatorNickname(authorNickname(creatorNickname));
		return toResponse(place, eventMap(List.of(place)));
	}

	public void delete(String id) {
		foodieRepository.delete(load(id));
	}

	private FoodiePlace load(String id) {
		return foodieRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Foodie place not found: " + id));
	}

	private void validateEvent(String eventId) {
		String normalized = blankToNull(eventId);
		if (normalized != null && !eventRepository.existsById(normalized)) {
			throw new EntityNotFoundException("Date event not found: " + normalized);
		}
	}

	private List<FoodiePlaceResponse> toResponses(List<FoodiePlace> places) {
		Map<String, DateEvent> events = eventMap(places);
		return places.stream().map(place -> toResponse(place, events)).toList();
	}

	private FoodiePlaceResponse toResponse(FoodiePlace place, Map<String, DateEvent> events) {
		DateEvent event = events.get(place.getEventId());
		return new FoodiePlaceResponse(
				place.getId(),
				place.getKakaoPlaceId(),
				place.getName(),
				place.getCategoryName(),
				place.getAddressName(),
				place.getRoadAddressName(),
				place.getPhone(),
				place.getPlaceUrl(),
				place.getLatitude(),
				place.getLongitude(),
				place.getKakaoRating(),
				place.getKakaoReviewCount(),
				place.getEventId(),
				event == null ? null : event.getTitle(),
				event == null ? null : event.getDate(),
				place.getVisitDate(),
				place.getPhotoUrl(),
				place.getOneLineReview(),
				place.getJunhongRating(),
				place.getSominRating(),
				place.getJunhongReview(),
				place.getSominReview(),
				authorId(place.getCreatedBy()),
				authorNickname(place.getCreatorNickname()),
				place.getUpdatedAt());
	}

	private Map<String, DateEvent> eventMap(List<FoodiePlace> places) {
		List<String> eventIds = places.stream()
				.map(FoodiePlace::getEventId)
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
