package com.junhong.somin.foodie;

import com.junhong.somin.common.KoreaTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "foodie_places")
public class FoodiePlace {

	@Id
	private String id;

	@Column(length = 80)
	private String kakaoPlaceId;
	@Column(length = 160)
	private String name;
	@Column(length = 255)
	private String categoryName;
	@Column(length = 255)
	private String addressName;
	@Column(length = 255)
	private String roadAddressName;
	@Column(length = 40)
	private String phone;
	@Column(length = 500)
	private String placeUrl;
	private Double latitude;
	private Double longitude;
	private Double kakaoRating;
	private Integer kakaoReviewCount;
	@Column(length = 40)
	private String eventId;
	private LocalDate visitDate;
	@Column(length = 1000)
	private String photoUrl;
	@Column(length = 500)
	private String oneLineReview;
	private Double junhongRating;
	private Double sominRating;
	@Column(length = 500)
	private String junhongReview;
	@Column(length = 500)
	private String sominReview;
	@Column(length = 40)
	private String createdBy;
	@Column(length = 40)
	private String creatorNickname;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	protected FoodiePlace() {
	}

	public FoodiePlace(
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
			String photoUrl,
			String oneLineReview,
			Double junhongRating,
			Double sominRating,
			String junhongReview,
			String sominReview,
			String createdBy,
			String creatorNickname) {
		this.id = UUID.randomUUID().toString();
		this.kakaoPlaceId = kakaoPlaceId;
		this.name = name;
		this.categoryName = categoryName;
		this.addressName = addressName;
		this.roadAddressName = roadAddressName;
		this.phone = phone;
		this.placeUrl = placeUrl;
		this.latitude = latitude;
		this.longitude = longitude;
		this.kakaoRating = kakaoRating;
		this.kakaoReviewCount = kakaoReviewCount;
		this.eventId = eventId;
		this.visitDate = visitDate;
		this.photoUrl = photoUrl;
		this.oneLineReview = oneLineReview;
		this.junhongRating = junhongRating;
		this.sominRating = sominRating;
		this.junhongReview = junhongReview;
		this.sominReview = sominReview;
		this.createdBy = createdBy;
		this.creatorNickname = creatorNickname;
	}

	@PrePersist
	void prePersist() {
		LocalDateTime now = KoreaTime.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = KoreaTime.now();
	}

	public String getId() {
		return id;
	}

	public String getKakaoPlaceId() {
		return kakaoPlaceId;
	}

	public void setKakaoPlaceId(String kakaoPlaceId) {
		this.kakaoPlaceId = kakaoPlaceId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	public String getAddressName() {
		return addressName;
	}

	public void setAddressName(String addressName) {
		this.addressName = addressName;
	}

	public String getRoadAddressName() {
		return roadAddressName;
	}

	public void setRoadAddressName(String roadAddressName) {
		this.roadAddressName = roadAddressName;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getPlaceUrl() {
		return placeUrl;
	}

	public void setPlaceUrl(String placeUrl) {
		this.placeUrl = placeUrl;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public Double getKakaoRating() {
		return kakaoRating;
	}

	public void setKakaoRating(Double kakaoRating) {
		this.kakaoRating = kakaoRating;
	}

	public Integer getKakaoReviewCount() {
		return kakaoReviewCount;
	}

	public void setKakaoReviewCount(Integer kakaoReviewCount) {
		this.kakaoReviewCount = kakaoReviewCount;
	}

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	public LocalDate getVisitDate() {
		return visitDate;
	}

	public void setVisitDate(LocalDate visitDate) {
		this.visitDate = visitDate;
	}

	public String getPhotoUrl() {
		return photoUrl;
	}

	public void setPhotoUrl(String photoUrl) {
		this.photoUrl = photoUrl;
	}

	public String getOneLineReview() {
		return oneLineReview;
	}

	public void setOneLineReview(String oneLineReview) {
		this.oneLineReview = oneLineReview;
	}

	public Double getJunhongRating() {
		return junhongRating;
	}

	public void setJunhongRating(Double junhongRating) {
		this.junhongRating = junhongRating;
	}

	public Double getSominRating() {
		return sominRating;
	}

	public void setSominRating(Double sominRating) {
		this.sominRating = sominRating;
	}

	public String getJunhongReview() {
		return junhongReview;
	}

	public void setJunhongReview(String junhongReview) {
		this.junhongReview = junhongReview;
	}

	public String getSominReview() {
		return sominReview;
	}

	public void setSominReview(String sominReview) {
		this.sominReview = sominReview;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getCreatorNickname() {
		return creatorNickname;
	}

	public void setCreatorNickname(String creatorNickname) {
		this.creatorNickname = creatorNickname;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
