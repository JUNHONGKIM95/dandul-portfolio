package com.junhong.somin.media;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaItemRepository extends JpaRepository<MediaItem, String> {
	List<MediaItem> findAllByOrderByCreatedAtDesc();

	List<MediaItem> findByFavoriteTrueOrderByCreatedAtDesc();

	List<MediaItem> findByEventIdOrderByCreatedAtDesc(String eventId);
}
