package com.junhong.somin.foodie;

import com.junhong.somin.foodie.FoodieDtos.FoodiePlaceResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/foodie-places")
public class FoodiePlaceController {

	private final FoodiePlaceService service;

	public FoodiePlaceController(FoodiePlaceService service) {
		this.service = service;
	}

	@GetMapping
	public List<FoodiePlaceResponse> findPlaces() {
		return service.findPlaces();
	}

	@PostMapping
	public FoodiePlaceResponse create(
			@RequestPart(required = false) MultipartFile photo,
			@RequestParam(required = false) String kakaoPlaceId,
			@RequestParam String name,
			@RequestParam(required = false) String categoryName,
			@RequestParam(required = false) String addressName,
			@RequestParam(required = false) String roadAddressName,
			@RequestParam(required = false) String phone,
			@RequestParam(required = false) String placeUrl,
			@RequestParam(required = false) Double latitude,
			@RequestParam(required = false) Double longitude,
			@RequestParam(required = false) Double kakaoRating,
			@RequestParam(required = false) Integer kakaoReviewCount,
			@RequestParam(required = false) String eventId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate visitDate,
			@RequestParam(required = false) String oneLineReview,
			@RequestParam(required = false) Double junhongRating,
			@RequestParam(required = false) Double sominRating,
			@RequestParam(required = false) String junhongReview,
			@RequestParam(required = false) String sominReview,
			@RequestParam(required = false) String createdBy,
			@RequestParam(required = false) String creatorNickname) {
		return service.create(photo, kakaoPlaceId, name, categoryName, addressName, roadAddressName, phone, placeUrl,
				latitude, longitude, kakaoRating, kakaoReviewCount, eventId, visitDate, oneLineReview, junhongRating,
				sominRating, junhongReview, sominReview, createdBy, creatorNickname);
	}

	@PutMapping("/{id}")
	public FoodiePlaceResponse update(
			@PathVariable String id,
			@RequestPart(required = false) MultipartFile photo,
			@RequestParam(required = false) String kakaoPlaceId,
			@RequestParam String name,
			@RequestParam(required = false) String categoryName,
			@RequestParam(required = false) String addressName,
			@RequestParam(required = false) String roadAddressName,
			@RequestParam(required = false) String phone,
			@RequestParam(required = false) String placeUrl,
			@RequestParam(required = false) Double latitude,
			@RequestParam(required = false) Double longitude,
			@RequestParam(required = false) Double kakaoRating,
			@RequestParam(required = false) Integer kakaoReviewCount,
			@RequestParam(required = false) String eventId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate visitDate,
			@RequestParam(required = false) String oneLineReview,
			@RequestParam(required = false) Double junhongRating,
			@RequestParam(required = false) Double sominRating,
			@RequestParam(required = false) String junhongReview,
			@RequestParam(required = false) String sominReview,
			@RequestParam(required = false) String createdBy,
			@RequestParam(required = false) String creatorNickname) {
		return service.update(id, photo, kakaoPlaceId, name, categoryName, addressName, roadAddressName, phone, placeUrl,
				latitude, longitude, kakaoRating, kakaoReviewCount, eventId, visitDate, oneLineReview, junhongRating,
				sominRating, junhongReview, sominReview, createdBy, creatorNickname);
	}

	@DeleteMapping("/{id}")
	public void delete(@PathVariable String id) {
		service.delete(id);
	}
}
