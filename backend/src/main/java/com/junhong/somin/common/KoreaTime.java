package com.junhong.somin.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class KoreaTime {

	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

	private KoreaTime() {
	}

	public static LocalDate today() {
		return LocalDate.now(ZONE);
	}

	public static LocalDateTime now() {
		return LocalDateTime.now(ZONE);
	}

	public static ZoneId zone() {
		return ZONE;
	}
}
