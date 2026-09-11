package com.junhong.somin.hiking;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public final class HikingDtos {

	private HikingDtos() {
	}

	public record HikingRecordRequest(
			@NotBlank @Size(max = 120) String mountainName,
			@NotBlank @Size(max = 255) String location,
			@Min(1) @Max(3000) int elevationMeter,
			@NotNull LocalDate climbedAt,
			Double latitude,
			Double longitude,
			@Size(max = 20) String source,
			@Size(max = 1000) String memo,
			@Size(max = 40) String createdBy,
			@Size(max = 40) String creatorNickname) {
	}

	public record HikingRecordResponse(
			String id,
			String mountainName,
			String location,
			int elevationMeter,
			LocalDate climbedAt,
			Double latitude,
			Double longitude,
			String source,
			String memo,
			String createdBy,
			String creatorNickname) {
	}
}
