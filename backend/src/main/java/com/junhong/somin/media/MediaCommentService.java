package com.junhong.somin.media;

import com.junhong.somin.media.MediaDtos.CommentRequest;
import com.junhong.somin.media.MediaDtos.CommentResponse;
import com.junhong.somin.notification.PushNotificationService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MediaCommentService {

	private final MediaItemRepository mediaRepository;
	private final MediaCommentRepository commentRepository;
	private final PushNotificationService pushNotificationService;

	public MediaCommentService(
			MediaItemRepository mediaRepository,
			MediaCommentRepository commentRepository,
			PushNotificationService pushNotificationService) {
		this.mediaRepository = mediaRepository;
		this.commentRepository = commentRepository;
		this.pushNotificationService = pushNotificationService;
	}

	@Transactional(readOnly = true)
	public List<CommentResponse> findAllComments() {
		return commentRepository.findAllByOrderByCreatedAtAsc().stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<CommentResponse> findComments(String mediaItemId) {
		validateMedia(mediaItemId);
		return commentRepository.findByMediaItemIdOrderByCreatedAtAsc(mediaItemId).stream()
				.map(this::toResponse)
				.toList();
	}

	public CommentResponse createComment(String mediaItemId, CommentRequest request) {
		MediaItem mediaItem = loadMedia(mediaItemId);
		MediaComment comment = new MediaComment(
				mediaItemId,
				request.content().trim(),
				authorId(request.createdBy()),
				authorNickname(request.creatorNickname()));
		MediaComment savedComment = commentRepository.save(comment);
		pushNotificationService.sendMediaCommentNotification(mediaItem, savedComment);
		return toResponse(savedComment);
	}

	public CommentResponse updateComment(String mediaItemId, String commentId, CommentRequest request) {
		validateMedia(mediaItemId);
		MediaComment comment = findComment(mediaItemId, commentId);
		comment.updateContent(request.content().trim());
		return toResponse(comment);
	}

	public void deleteComment(String mediaItemId, String commentId) {
		validateMedia(mediaItemId);
		MediaComment comment = findComment(mediaItemId, commentId);
		commentRepository.delete(comment);
	}

	private void validateMedia(String mediaItemId) {
		loadMedia(mediaItemId);
	}

	private MediaItem loadMedia(String mediaItemId) {
		return mediaRepository.findById(mediaItemId)
				.orElseThrow(() -> new EntityNotFoundException("Media item not found: " + mediaItemId));
	}

	private MediaComment findComment(String mediaItemId, String commentId) {
		MediaComment comment = commentRepository.findById(commentId)
				.orElseThrow(() -> new EntityNotFoundException("Comment not found: " + commentId));
		if (!comment.getMediaItemId().equals(mediaItemId)) {
			throw new EntityNotFoundException("Comment not found: " + commentId);
		}
		return comment;
	}

	private String authorId(String value) {
		return value == null || value.isBlank() ? "junhong" : value;
	}

	private String authorNickname(String value) {
		return value == null || value.isBlank() ? "준홍" : value;
	}

	private CommentResponse toResponse(MediaComment comment) {
		return new CommentResponse(
				comment.getId(),
				comment.getMediaItemId(),
				comment.getContent(),
				comment.getCreatedBy(),
				comment.getCreatorNickname(),
				comment.getCreatedAt());
	}
}
