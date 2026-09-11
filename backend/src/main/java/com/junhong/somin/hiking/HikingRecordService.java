package com.junhong.somin.hiking;

import com.junhong.somin.hiking.HikingDtos.HikingRecordRequest;
import com.junhong.somin.hiking.HikingDtos.HikingRecordResponse;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class HikingRecordService {

	private final HikingRecordRepository repository;

	public HikingRecordService(HikingRecordRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public List<HikingRecordResponse> findRecords(LocalDate start, LocalDate end) {
		List<HikingRecord> records = start != null && end != null
				? repository.findByClimbedAtBetweenOrderByClimbedAtDesc(start, end)
				: repository.findAllByOrderByClimbedAtDesc();
		return records.stream().map(this::toResponse).toList();
	}

	public HikingRecordResponse create(HikingRecordRequest request) {
		HikingRecord record = new HikingRecord(
				request.mountainName(),
				request.location(),
				request.elevationMeter(),
				request.climbedAt(),
				request.latitude(),
				request.longitude(),
				request.source(),
				request.memo(),
				authorId(request.createdBy()),
				authorNickname(request.creatorNickname()));
		return toResponse(repository.save(record));
	}

	public HikingRecordResponse update(String id, HikingRecordRequest request) {
		HikingRecord record = load(id);
		record.setMountainName(request.mountainName());
		record.setLocation(request.location());
		record.setElevationMeter(request.elevationMeter());
		record.setClimbedAt(request.climbedAt());
		record.setLatitude(request.latitude());
		record.setLongitude(request.longitude());
		record.setSource(request.source());
		record.setMemo(request.memo());
		record.setCreatedBy(authorId(request.createdBy()));
		record.setCreatorNickname(authorNickname(request.creatorNickname()));
		return toResponse(record);
	}

	public void delete(String id) {
		repository.delete(load(id));
	}

	private HikingRecord load(String id) {
		return repository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Hiking record not found: " + id));
	}

	private HikingRecordResponse toResponse(HikingRecord record) {
		return new HikingRecordResponse(
				record.getId(),
				record.getMountainName(),
				record.getLocation(),
				record.getElevationMeter(),
				record.getClimbedAt(),
				record.getLatitude(),
				record.getLongitude(),
				record.getSource(),
				record.getMemo(),
				authorId(record.getCreatedBy()),
				authorNickname(record.getCreatorNickname()));
	}

	private String authorId(String value) {
		return value == null || value.isBlank() ? "junhong" : value;
	}

	private String authorNickname(String value) {
		return value == null || value.isBlank() ? "준홍" : value;
	}
}
