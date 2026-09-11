package com.junhong.somin.profile;

import com.junhong.somin.profile.ProfileDtos.ProfileResponse;
import com.junhong.somin.profile.ProfileDtos.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
public class CoupleProfileController {

	private final CoupleProfileService service;

	public CoupleProfileController(CoupleProfileService service) {
		this.service = service;
	}

	@GetMapping
	public ProfileResponse getProfile() {
		return service.getProfile();
	}

	@PutMapping
	public ProfileResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
		return service.updateProfile(request);
	}

	@PostMapping("/cover")
	public ProfileResponse updateCoverPhoto(@RequestPart("file") MultipartFile file) {
		return service.updateCoverPhoto(file);
	}

	@PostMapping("/splash")
	public ProfileResponse updateSplashPhoto(@RequestPart("file") MultipartFile file) {
		return service.updateSplashPhoto(file);
	}
}
