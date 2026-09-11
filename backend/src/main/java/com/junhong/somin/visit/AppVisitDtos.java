package com.junhong.somin.visit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AppVisitDtos {

	private AppVisitDtos() {
	}

	public record VisitRequest(
			@NotBlank @Size(max = 40) String username,
			@Size(max = 40) String nickname) {
	}
}
