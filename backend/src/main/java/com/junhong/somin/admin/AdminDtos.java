package com.junhong.somin.admin;

import java.util.List;

public final class AdminDtos {

	private AdminDtos() {
	}

	public record AdminMetricsResponse(
			Summary summary,
			List<BucketCount> monthlyPosts,
			List<BucketCount> monthlyComments,
			List<BucketCount> monthlyTogetherEvents,
			List<UserCount> visitsByUser,
			List<BucketUserCount> monthlyVisits,
			List<UserCount> contentByUser,
			List<BucketCount> mediaTypeCounts,
			List<BucketCount> monthlyHikingRecords) {
	}

	public record Summary(
			long totalPosts,
			long totalComments,
			long togetherEvents,
			long totalVisits,
			long favoritePosts,
			long hikingRecords,
			long totalElevationMeter) {
	}

	public record BucketCount(String label, long count) {
	}

	public record UserCount(String username, String nickname, long count) {
	}

	public record BucketUserCount(String label, long junhong, long somin) {
	}
}
