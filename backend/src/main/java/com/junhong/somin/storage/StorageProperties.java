package com.junhong.somin.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
		String mode,
		String localRoot,
		String localPublicBaseUrl,
		String supabaseUrl,
		String supabaseServiceRoleKey,
		String supabaseBucket) {
}
