package com.junhong.somin.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public final class DateEventDtos {

	private DateEventDtos() {
	}

	public record EventRequest(
			@NotNull LocalDate date,
			LocalDate endDate,
			LocalTime meetingTime,
			@NotBlank @Size(max = 255) String place,
			@NotBlank @Size(max = 120) String title,
			@Size(max = 2000) String memo,
			@Size(max = 20) String category,
			@Size(max = 40) String createdBy,
			@Size(max = 40) String creatorNickname) {
	}

	public record EventResponse(
			String id,
			LocalDate date,
			LocalDate endDate,
			LocalTime meetingTime,
			String place,
			String title,
			String memo,
			String category,
			String createdBy,
			String creatorNickname) {
	}
}
