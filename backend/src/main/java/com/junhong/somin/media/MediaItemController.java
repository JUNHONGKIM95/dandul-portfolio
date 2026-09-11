package com.junhong.somin.media;

import com.junhong.somin.media.MediaDtos.FavoriteRequest;
import com.junhong.somin.media.MediaDtos.CommentRequest;
import com.junhong.somin.media.MediaDtos.CommentResponse;
import com.junhong.somin.media.MediaDtos.MediaResponse;
import com.junhong.somin.media.MediaDtos.RotateRequest;
import com.junhong.somin.media.MediaDtos.UpdateMediaRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
public class MediaItemController {

	private final MediaItemService service;
	private final MediaCommentService commentService;

	public MediaItemController(MediaItemService service, MediaCommentService commentService) {
		this.service = service;
		this.commentService = commentService;
	}

	@GetMapping
	public List<MediaResponse> findMedia(
			@RequestParam(required = false) Boolean favorite,
			@RequestParam(required = false) String eventId) {
		return service.findMedia(favorite, eventId);
	}

	@PostMapping
	public MediaResponse create(
			@RequestPart("file") MultipartFile file,
			@RequestParam MediaType mediaType,
			@RequestParam(required = false) String eventId,
			@RequestParam(required = false) String title,
			@RequestParam(required = false) String memo,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate capturedAt,
			@RequestParam(required = false) String createdBy,
			@RequestParam(required = false) String creatorNickname,
			@RequestParam(defaultValue = "0") int rotationDegrees) {
		return service.create(file, mediaType, eventId, title, memo, capturedAt, createdBy, creatorNickname,
				rotationDegrees);
	}

	@PutMapping("/{id}")
	public MediaResponse update(@PathVariable String id, @Valid @RequestBody UpdateMediaRequest request) {
		return service.update(id, request);
	}

	@PatchMapping("/{id}/favorite")
	public MediaResponse updateFavorite(@PathVariable String id, @RequestBody FavoriteRequest request) {
		return service.updateFavorite(id, request);
	}

	@PatchMapping("/{id}/rotate")
	public MediaResponse rotate(@PathVariable String id, @RequestBody RotateRequest request) {
		return service.rotate(id, request);
	}

	@DeleteMapping("/{id}")
	public void delete(@PathVariable String id) {
		service.delete(id);
	}

	@GetMapping("/{id}/comments")
	public List<CommentResponse> findComments(@PathVariable String id) {
		return commentService.findComments(id);
	}

	@PostMapping("/{id}/comments")
	public CommentResponse createComment(@PathVariable String id, @Valid @RequestBody CommentRequest request) {
		return commentService.createComment(id, request);
	}

	@RequestMapping(path = "/{id}/comments/{commentId}", method = { RequestMethod.PUT, RequestMethod.PATCH })
	public CommentResponse updateComment(
			@PathVariable String id,
			@PathVariable String commentId,
			@Valid @RequestBody CommentRequest request) {
		return commentService.updateComment(id, commentId, request);
	}

	@DeleteMapping("/{id}/comments/{commentId}")
	public void deleteComment(@PathVariable String id, @PathVariable String commentId) {
		commentService.deleteComment(id, commentId);
	}
}
