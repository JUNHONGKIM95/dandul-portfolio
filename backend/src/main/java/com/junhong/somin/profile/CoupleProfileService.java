package com.junhong.somin.profile;

import com.junhong.somin.common.KoreaTime;
import com.junhong.somin.profile.ProfileDtos.ProfileResponse;
import com.junhong.somin.profile.ProfileDtos.UpdateProfileRequest;
import com.junhong.somin.storage.FileStorageService;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class CoupleProfileService {

	private static final String DEFAULT_ID = "default";

	private final CoupleProfileRepository repository;
	private final FileStorageService storageService;

	public CoupleProfileService(CoupleProfileRepository repository, FileStorageService storageService) {
		this.repository = repository;
		this.storageService = storageService;
	}

	@Transactional(readOnly = true)
	public ProfileResponse getProfile() {
		return toResponse(loadProfile());
	}

	public ProfileResponse updateProfile(UpdateProfileRequest request) {
		CoupleProfile profile = loadProfile();
		profile.setBoyfriendName(request.boyfriendName());
		profile.setGirlfriendName(request.girlfriendName());
		profile.setRelationshipStartDate(request.relationshipStartDate());
		profile.setCoverPhotoUrl(request.coverPhotoUrl());
		return toResponse(profile);
	}

	public ProfileResponse updateCoverPhoto(MultipartFile file) {
		CoupleProfile profile = loadProfile();
		profile.setCoverPhotoUrl(storageService.store(file, "profile"));
		return toResponse(profile);
	}

	public ProfileResponse updateSplashPhoto(MultipartFile file) {
		if (file == null || file.isEmpty() || file.getContentType() == null
				|| !file.getContentType().startsWith("image/")) {
			throw new IllegalArgumentException("스플래시 화면에는 이미지 파일만 등록할 수 있어요.");
		}
		CoupleProfile profile = loadProfile();
		profile.setSplashPhotoUrl(storageService.store(file, "splash"));
		return toResponse(profile);
	}

	private CoupleProfile loadProfile() {
		return repository.findById(DEFAULT_ID)
				.orElseGet(() -> repository.save(new CoupleProfile(
						DEFAULT_ID,
						"김준홍",
						"전소민",
						LocalDate.of(2024, 3, 30))));
	}

	private ProfileResponse toResponse(CoupleProfile profile) {
		long daysTogether = ChronoUnit.DAYS.between(profile.getRelationshipStartDate(), KoreaTime.today()) + 1;
		return new ProfileResponse(
				profile.getId(),
				profile.getBoyfriendName(),
				profile.getGirlfriendName(),
				profile.getRelationshipStartDate(),
				profile.getCoverPhotoUrl(),
				profile.getSplashPhotoUrl(),
				daysTogether);
	}
}
