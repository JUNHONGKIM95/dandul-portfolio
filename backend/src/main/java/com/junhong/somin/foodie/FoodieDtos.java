package com.junhong.somin.foodie;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class FoodieDtos {

	private FoodieDtos() {
	}

	public record FoodiePlaceResponse(
			String id,
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
			String eventTitle,
			LocalDate eventDate,
			LocalDate visitDate,
			String photoUrl,
			String oneLineReview,
			Double junhongRating,
			Double sominRating,
			String junhongReview,
			String sominReview,
			String createdBy,
			String creatorNickname,
			LocalDateTime updatedAt) {
	}
}
