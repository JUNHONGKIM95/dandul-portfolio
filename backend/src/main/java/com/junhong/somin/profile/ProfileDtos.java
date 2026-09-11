package com.junhong.somin.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public final class ProfileDtos {

	private ProfileDtos() {
	}

	public record ProfileResponse(
			String id,
			String boyfriendName,
			String girlfriendName,
			LocalDate relationshipStartDate,
			String coverPhotoUrl,
			String splashPhotoUrl,
			long daysTogether) {
	}

	public record UpdateProfileRequest(
			@NotBlank String boyfriendName,
			@NotBlank String girlfriendName,
			@NotNull LocalDate relationshipStartDate,
			String coverPhotoUrl) {
	}
}
