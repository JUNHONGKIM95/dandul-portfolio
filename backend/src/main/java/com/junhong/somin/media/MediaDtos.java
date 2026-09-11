package com.junhong.somin.media;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class MediaDtos {

	private MediaDtos() {
	}

	public record MediaResponse(
			String id,
			String eventId,
			String eventTitle,
			LocalDate eventDate,
			MediaType mediaType,
			String url,
			String originalFileName,
			String title,
			String memo,
			boolean favorite,
			LocalDate capturedAt,
			String createdBy,
			String creatorNickname,
			LocalDateTime updatedAt) {
	}

	public record UpdateMediaRequest(
			String eventId,
			@Size(max = 120) String title,
			@Size(max = 2000) String memo,
			LocalDate capturedAt) {
	}

	public record FavoriteRequest(boolean favorite) {
	}

	public record RotateRequest(int rotationDegrees) {
	}

	public record CommentRequest(
			@NotBlank @Size(max = 500) String content,
			@Size(max = 40) String createdBy,
			@Size(max = 40) String creatorNickname) {
	}

	public record CommentResponse(
			String id,
			String mediaItemId,
			String content,
			String createdBy,
			String creatorNickname,
			LocalDateTime createdAt) {
	}
}
