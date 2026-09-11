package com.junhong.somin.media;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaCommentRepository extends JpaRepository<MediaComment, String> {
	List<MediaComment> findAllByOrderByCreatedAtAsc();

	List<MediaComment> findByMediaItemIdOrderByCreatedAtAsc(String mediaItemId);

	void deleteByMediaItemId(String mediaItemId);
}
