package com.junhong.somin.admin;

import com.junhong.somin.admin.AdminDtos.AdminMetricsResponse;
import com.junhong.somin.admin.AdminDtos.BucketCount;
import com.junhong.somin.admin.AdminDtos.BucketUserCount;
import com.junhong.somin.admin.AdminDtos.Summary;
import com.junhong.somin.admin.AdminDtos.UserCount;
import com.junhong.somin.common.KoreaTime;
import com.junhong.somin.event.DateEvent;
import com.junhong.somin.event.DateEventRepository;
import com.junhong.somin.hiking.HikingRecord;
import com.junhong.somin.hiking.HikingRecordRepository;
import com.junhong.somin.media.MediaComment;
import com.junhong.somin.media.MediaCommentRepository;
import com.junhong.somin.media.MediaItem;
import com.junhong.somin.media.MediaItemRepository;
import com.junhong.somin.visit.AppVisit;
import com.junhong.somin.visit.AppVisitRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminMetricsService {

	private final DateEventRepository eventRepository;
	private final MediaItemRepository mediaRepository;
	private final MediaCommentRepository commentRepository;
	private final HikingRecordRepository hikingRepository;
	private final AppVisitRepository visitRepository;

	public AdminMetricsService(
			DateEventRepository eventRepository,
			MediaItemRepository mediaRepository,
			MediaCommentRepository commentRepository,
			HikingRecordRepository hikingRepository,
			AppVisitRepository visitRepository) {
		this.eventRepository = eventRepository;
		this.mediaRepository = mediaRepository;
		this.commentRepository = commentRepository;
		this.hikingRepository = hikingRepository;
		this.visitRepository = visitRepository;
	}

	@Transactional(readOnly = true)
	public AdminMetricsResponse metrics(LocalDate startDate, LocalDate endDate) {
		DateRange range = normalizeRange(startDate, endDate);
		List<DateEvent> events = eventRepository.findAll().stream()
				.filter(event -> eventOverlapsRange(event, range))
				.toList();
		List<MediaItem> media = mediaRepository.findAll().stream()
				.filter(item -> item.getCreatedAt() != null && isInRange(item.getCreatedAt().toLocalDate(), range))
				.toList();
		List<MediaComment> comments = commentRepository.findAll().stream()
				.filter(comment -> comment.getCreatedAt() != null && isInRange(comment.getCreatedAt().toLocalDate(), range))
				.toList();
		List<HikingRecord> hikes = hikingRepository.findAll().stream()
				.filter(record -> record.getClimbedAt() != null && isInRange(record.getClimbedAt(), range))
				.toList();
		List<AppVisit> visits = visitRepository.findAll().stream()
				.filter(visit -> visit.getVisitedAt() != null && isInRange(visit.getVisitedAt().toLocalDate(), range))
				.toList();

		long togetherEvents = events.stream().filter(this::isTogetherEvent).count();
		long totalElevation = hikes.stream().mapToLong(HikingRecord::getElevationMeter).sum();

		return new AdminMetricsResponse(
				new Summary(
						media.size(),
						comments.size(),
						togetherEvents,
						visits.size(),
						media.stream().filter(MediaItem::isFavorite).count(),
						hikes.size(),
						totalElevation),
				monthlyPostCounts(media, range),
				monthlyCommentCounts(comments, range),
				monthlyTogetherEventCounts(events, range),
				userCounts(visits.stream().map(AppVisit::getUsername).toList()),
				monthlyVisitCounts(visits, range),
				contentByUser(media, comments, hikes),
				mediaTypeCounts(media),
				monthlyHikingCounts(hikes, range));
	}

	private List<BucketCount> monthlyPostCounts(List<MediaItem> media, DateRange range) {
		Map<String, Long> buckets = monthBuckets(range);
		media.forEach(item -> {
			if (item.getCreatedAt() != null) {
				incrementMonth(buckets, YearMonth.from(item.getCreatedAt()));
			}
		});
		return bucketCounts(buckets);
	}

	private List<BucketCount> monthlyCommentCounts(List<MediaComment> comments, DateRange range) {
		Map<String, Long> buckets = monthBuckets(range);
		comments.forEach(comment -> {
			if (comment.getCreatedAt() != null) {
				incrementMonth(buckets, YearMonth.from(comment.getCreatedAt()));
			}
		});
		return bucketCounts(buckets);
	}

	private List<BucketCount> monthlyTogetherEventCounts(List<DateEvent> events, DateRange range) {
		Map<String, Long> buckets = monthBuckets(range);
		events.stream().filter(this::isTogetherEvent).forEach(event -> {
			if (event.getDate() != null) {
				incrementMonth(buckets, YearMonth.from(event.getDate()));
			}
		});
		return bucketCounts(buckets);
	}

	private List<BucketUserCount> monthlyVisitCounts(List<AppVisit> visits, DateRange range) {
		Map<String, long[]> buckets = new LinkedHashMap<>();
		monthBuckets(range).keySet().forEach(label -> buckets.put(label, new long[] { 0, 0 }));
		visits.forEach(visit -> {
			if (visit.getVisitedAt() == null) {
				return;
			}
			long[] counts = buckets.get("%d월".formatted(YearMonth.from(visit.getVisitedAt()).getMonthValue()));
			if (counts == null) {
				return;
			}
			if ("somin".equals(visit.getUsername())) {
				counts[1] += 1;
			} else if ("junhong".equals(visit.getUsername())) {
				counts[0] += 1;
			}
		});
		return buckets.entrySet().stream()
				.map(entry -> new BucketUserCount(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
				.toList();
	}

	private List<UserCount> contentByUser(List<MediaItem> media, List<MediaComment> comments, List<HikingRecord> hikes) {
		List<String> users = new ArrayList<>();
		media.forEach(item -> users.add(item.getCreatedBy()));
		comments.forEach(comment -> users.add(comment.getCreatedBy()));
		hikes.forEach(record -> users.add(record.getCreatedBy()));
		return userCounts(users);
	}

	private List<BucketCount> mediaTypeCounts(List<MediaItem> media) {
		Map<String, Long> buckets = new LinkedHashMap<>();
		buckets.put("PHOTO", 0L);
		buckets.put("VIDEO", 0L);
		media.forEach(item -> increment(buckets, item.getMediaType() == null ? "PHOTO" : item.getMediaType().name()));
		return bucketCounts(buckets);
	}

	private List<BucketCount> monthlyHikingCounts(List<HikingRecord> hikes, DateRange range) {
		Map<String, Long> buckets = monthBuckets(range);
		hikes.forEach(record -> {
			if (record.getClimbedAt() != null) {
				incrementMonth(buckets, YearMonth.from(record.getClimbedAt()));
			}
		});
		return bucketCounts(buckets);
	}

	private List<UserCount> userCounts(List<String> usernames) {
		Map<String, Long> counts = new LinkedHashMap<>();
		counts.put("junhong", 0L);
		counts.put("somin", 0L);
		usernames.forEach(username -> {
			if ("junhong".equals(username) || "somin".equals(username)) {
				increment(counts, username);
			}
		});
		return counts.entrySet().stream()
				.map(entry -> new UserCount(entry.getKey(), nickname(entry.getKey()), entry.getValue()))
				.toList();
	}

	private Map<String, Long> monthBuckets(DateRange range) {
		Map<String, Long> buckets = new LinkedHashMap<>();
		for (int month = 1; month <= 12; month += 1) {
			buckets.put("%d월".formatted(month), 0L);
		}
		return buckets;
	}

	private DateRange normalizeRange(LocalDate startDate, LocalDate endDate) {
		LocalDate start = startDate == null ? LocalDate.of(1900, 1, 1) : startDate;
		LocalDate end = endDate == null ? LocalDate.of(2999, 12, 31) : endDate;
		if (end.isBefore(start)) {
			return new DateRange(end, start);
		}
		return new DateRange(start, end);
	}

	private boolean isInRange(LocalDate date, DateRange range) {
		return !date.isBefore(range.start()) && !date.isAfter(range.end());
	}

	private boolean eventOverlapsRange(DateEvent event, DateRange range) {
		if (event.getDate() == null) {
			return false;
		}
		LocalDate end = event.getEndDate() == null ? event.getDate() : event.getEndDate();
		return !event.getDate().isAfter(range.end()) && !end.isBefore(range.start());
	}

	private List<BucketCount> bucketCounts(Map<String, Long> buckets) {
		return buckets.entrySet().stream()
				.map(entry -> new BucketCount(entry.getKey(), entry.getValue()))
				.toList();
	}

	private void increment(Map<String, Long> buckets, String key) {
		buckets.computeIfPresent(key, (ignored, count) -> count + 1);
	}

	private void incrementMonth(Map<String, Long> buckets, YearMonth month) {
		increment(buckets, "%d월".formatted(month.getMonthValue()));
	}

	private boolean isTogetherEvent(DateEvent event) {
		return "together".equals(event.getCategory());
	}

	private String nickname(String username) {
		if ("somin".equals(username)) {
			return "소민";
		}
		return "준홍";
	}

	private record DateRange(LocalDate start, LocalDate end) {
	}
}
