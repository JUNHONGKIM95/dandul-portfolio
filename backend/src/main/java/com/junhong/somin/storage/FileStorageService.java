package com.junhong.somin.storage;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

	private final StorageProperties properties;
	private final HttpClient httpClient;

	public FileStorageService(StorageProperties properties) {
		this.properties = properties;
		this.httpClient = HttpClient.newHttpClient();
	}

	public String store(MultipartFile file, String folder) {
		return store(file, folder, 0);
	}

	public String store(MultipartFile file, String folder, int rotationDegrees) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("File is required.");
		}

		boolean normalizeImage = shouldNormalizeImage(file) && shouldNormalizeOnServer(rotationDegrees);
		String fileName = buildStoredFileName(file.getOriginalFilename(), normalizeImage);
		String objectPath = sanitize(folder) + "/" + fileName;

		if ("supabase".equalsIgnoreCase(properties.mode())) {
			return storeToSupabase(file, objectPath, normalizeImage, rotationDegrees);
		}
		return storeLocally(file, objectPath, normalizeImage, rotationDegrees);
	}

	public void rotateLocalPublicUrl(String publicUrl, int rotationDegrees) {
		if (!"local".equalsIgnoreCase(properties.mode()) && properties.mode() != null && !properties.mode().isBlank()) {
			throw new IllegalStateException("Server-side rotate is only available for local storage.");
		}
		int normalizedDegrees = Math.floorMod(rotationDegrees, 360);
		if (normalizedDegrees == 0) {
			return;
		}
		Path root = Path.of(properties.localRoot()).toAbsolutePath().normalize();
		String baseUrl = trimTrailingSlash(properties.localPublicBaseUrl()) + "/";
		if (publicUrl == null || !publicUrl.startsWith(baseUrl)) {
			throw new IllegalArgumentException("Only local uploaded files can be rotated.");
		}

		String objectPath = publicUrl.substring(baseUrl.length()).split("\\?", 2)[0];
		Path target = root.resolve(objectPath).normalize();
		if (!target.startsWith(root)) {
			throw new IllegalArgumentException("Invalid file path.");
		}
		try {
			BufferedImage source = ImageIO.read(target.toFile());
			if (source == null) {
				throw new IllegalArgumentException("Unsupported image file.");
			}
			Files.write(target, writeJpeg(toRgb(rotate(source, normalizedDegrees))));
		} catch (IOException ex) {
			throw new IllegalStateException("Could not rotate image.", ex);
		}
	}

	private String storeLocally(MultipartFile file, String objectPath, boolean normalizeImage, int rotationDegrees) {
		try {
			Path root = Path.of(properties.localRoot()).toAbsolutePath().normalize();
			Path target = root.resolve(objectPath).normalize();
			if (!target.startsWith(root)) {
				throw new IllegalArgumentException("Invalid file path.");
			}
			Files.createDirectories(target.getParent());
			if (normalizeImage) {
				Files.write(target, normalizeToJpeg(file, rotationDegrees));
			} else {
				Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
			}
			return trimTrailingSlash(properties.localPublicBaseUrl()) + "/" + objectPath.replace("\\", "/");
		} catch (IOException ex) {
			throw new IllegalStateException("Could not store file locally.", ex);
		}
	}

	private String storeToSupabase(MultipartFile file, String objectPath, boolean normalizeImage, int rotationDegrees) {
		if (isBlank(properties.supabaseUrl()) || isBlank(properties.supabaseServiceRoleKey())) {
			throw new IllegalStateException("Supabase storage settings are missing.");
		}

		try {
			String storedContentType = normalizeImage ? "image/jpeg" : contentType(file);
			String endpoint = "%s/storage/v1/object/%s/%s".formatted(
					trimTrailingSlash(properties.supabaseUrl()),
					properties.supabaseBucket(),
					objectPath);
			HttpRequest.BodyPublisher bodyPublisher = normalizeImage
					? HttpRequest.BodyPublishers.ofByteArray(normalizeToJpeg(file, rotationDegrees))
					: HttpRequest.BodyPublishers.ofInputStream(() -> {
						try {
							return file.getInputStream();
						} catch (IOException ex) {
							throw new UncheckedIOException(ex);
						}
					});
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(endpoint))
					.header("Authorization", "Bearer " + properties.supabaseServiceRoleKey())
					.header("apikey", properties.supabaseServiceRoleKey())
					.header("Content-Type", storedContentType)
					.header("x-upsert", "true")
					.POST(bodyPublisher)
					.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new IllegalStateException("Supabase upload failed: " + response.body());
			}
			return "%s/storage/v1/object/public/%s/%s".formatted(
					trimTrailingSlash(properties.supabaseUrl()),
					properties.supabaseBucket(),
					objectPath);
		} catch (IOException ex) {
			throw new IllegalStateException("Could not read uploaded file.", ex);
		} catch (UncheckedIOException ex) {
			throw new IllegalStateException("Could not stream uploaded file.", ex.getCause());
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Supabase upload was interrupted.", ex);
		}
	}

	private boolean shouldNormalizeOnServer(int rotationDegrees) {
		if ("supabase".equalsIgnoreCase(properties.mode())) {
			return Math.floorMod(rotationDegrees, 360) != 0;
		}
		return true;
	}

	private String buildStoredFileName(String originalName, boolean normalizeImage) {
		String cleanName = sanitize(originalName == null ? "upload" : originalName);
		if (normalizeImage) {
			return UUID.randomUUID() + ".jpg";
		}
		String extension = "";
		int dotIndex = cleanName.lastIndexOf('.');
		if (dotIndex >= 0) {
			extension = cleanName.substring(dotIndex).toLowerCase(Locale.ROOT);
		}
		return UUID.randomUUID() + extension;
	}

	private boolean shouldNormalizeImage(MultipartFile file) {
		String contentType = contentType(file).toLowerCase(Locale.ROOT);
		String originalName = file.getOriginalFilename() == null
				? ""
				: file.getOriginalFilename().toLowerCase(Locale.ROOT);
		boolean imageContentType = contentType.startsWith("image/")
				&& !contentType.equals("image/gif")
				&& !contentType.equals("image/svg+xml");
		boolean imageExtension = originalName.endsWith(".jpg")
				|| originalName.endsWith(".jpeg")
				|| originalName.endsWith(".png")
				|| originalName.endsWith(".webp");
		return imageContentType || imageExtension;
	}

	private byte[] normalizeToJpeg(MultipartFile file, int rotationDegrees) {
		try {
			byte[] imageBytes = file.getBytes();
			BufferedImage source = ImageIO.read(new ByteArrayInputStream(imageBytes));
			if (source == null) {
				throw new IllegalArgumentException("Unsupported image file.");
			}
			BufferedImage orientedImage = rotate(source, exifRotationDegrees(imageBytes) + rotationDegrees);
			BufferedImage rgbImage = toRgb(orientedImage);
			return writeJpeg(rgbImage);
		} catch (IOException ex) {
			throw new IllegalStateException("Could not normalize image.", ex);
		}
	}

	private BufferedImage toRgb(BufferedImage source) {
		BufferedImage rgbImage = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = rgbImage.createGraphics();
		try {
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, rgbImage.getWidth(), rgbImage.getHeight());
			graphics.drawImage(source, 0, 0, null);
		} finally {
			graphics.dispose();
		}
		return rgbImage;
	}

	private BufferedImage rotate(BufferedImage source, int degrees) {
		int normalizedDegrees = Math.floorMod(degrees, 360);
		if (normalizedDegrees == 0) {
			return source;
		}
		int sourceWidth = source.getWidth();
		int sourceHeight = source.getHeight();
		int targetWidth = normalizedDegrees == 180 ? sourceWidth : sourceHeight;
		int targetHeight = normalizedDegrees == 180 ? sourceHeight : sourceWidth;
		BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = target.createGraphics();
		try {
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			if (normalizedDegrees == 90) {
				graphics.translate(targetWidth, 0);
				graphics.rotate(Math.toRadians(90));
			} else if (normalizedDegrees == 180) {
				graphics.translate(targetWidth, targetHeight);
				graphics.rotate(Math.toRadians(180));
			} else if (normalizedDegrees == 270) {
				graphics.translate(0, targetHeight);
				graphics.rotate(Math.toRadians(270));
			}
			graphics.drawImage(source, 0, 0, null);
		} finally {
			graphics.dispose();
		}
		return target;
	}

	private int exifRotationDegrees(byte[] bytes) {
		int orientation = exifOrientation(bytes);
		return switch (orientation) {
			case 3 -> 180;
			case 6 -> 90;
			case 8 -> 270;
			default -> 0;
		};
	}

	private int exifOrientation(byte[] bytes) {
		if (bytes.length < 4 || unsignedByte(bytes[0]) != 0xFF || unsignedByte(bytes[1]) != 0xD8) {
			return 1;
		}

		int offset = 2;
		while (offset + 4 < bytes.length && unsignedByte(bytes[offset]) == 0xFF) {
			int marker = unsignedByte(bytes[offset + 1]);
			offset += 2;
			if (marker == 0xDA || marker == 0xD9 || offset + 2 > bytes.length) {
				break;
			}

			int segmentLength = readUnsignedShort(bytes, offset, false);
			if (segmentLength < 2 || offset + segmentLength > bytes.length) {
				break;
			}

			int segmentStart = offset + 2;
			if (marker == 0xE1 && segmentLength >= 8 && hasExifHeader(bytes, segmentStart)) {
				return readTiffOrientation(bytes, segmentStart + 6, offset + segmentLength);
			}
			offset += segmentLength;
		}
		return 1;
	}

	private boolean hasExifHeader(byte[] bytes, int offset) {
		return offset + 6 <= bytes.length
				&& bytes[offset] == 'E'
				&& bytes[offset + 1] == 'x'
				&& bytes[offset + 2] == 'i'
				&& bytes[offset + 3] == 'f'
				&& bytes[offset + 4] == 0
				&& bytes[offset + 5] == 0;
	}

	private int readTiffOrientation(byte[] bytes, int tiffStart, int segmentEnd) {
		if (tiffStart + 8 > segmentEnd) {
			return 1;
		}
		boolean littleEndian;
		if (bytes[tiffStart] == 'I' && bytes[tiffStart + 1] == 'I') {
			littleEndian = true;
		} else if (bytes[tiffStart] == 'M' && bytes[tiffStart + 1] == 'M') {
			littleEndian = false;
		} else {
			return 1;
		}

		int ifdOffset = readInt(bytes, tiffStart + 4, littleEndian);
		int ifdStart = tiffStart + ifdOffset;
		if (ifdStart < tiffStart || ifdStart + 2 > segmentEnd) {
			return 1;
		}

		int entryCount = readUnsignedShort(bytes, ifdStart, littleEndian);
		int entryOffset = ifdStart + 2;
		for (int i = 0; i < entryCount; i += 1) {
			int current = entryOffset + (i * 12);
			if (current + 12 > segmentEnd) {
				break;
			}
			int tag = readUnsignedShort(bytes, current, littleEndian);
			if (tag == 0x0112) {
				return readUnsignedShort(bytes, current + 8, littleEndian);
			}
		}
		return 1;
	}

	private int readUnsignedShort(byte[] bytes, int offset, boolean littleEndian) {
		if (littleEndian) {
			return unsignedByte(bytes[offset]) | (unsignedByte(bytes[offset + 1]) << 8);
		}
		return (unsignedByte(bytes[offset]) << 8) | unsignedByte(bytes[offset + 1]);
	}

	private int readInt(byte[] bytes, int offset, boolean littleEndian) {
		if (littleEndian) {
			return unsignedByte(bytes[offset])
					| (unsignedByte(bytes[offset + 1]) << 8)
					| (unsignedByte(bytes[offset + 2]) << 16)
					| (unsignedByte(bytes[offset + 3]) << 24);
		}
		return (unsignedByte(bytes[offset]) << 24)
				| (unsignedByte(bytes[offset + 1]) << 16)
				| (unsignedByte(bytes[offset + 2]) << 8)
				| unsignedByte(bytes[offset + 3]);
	}

	private int unsignedByte(byte value) {
		return value & 0xFF;
	}

	private byte[] writeJpeg(BufferedImage image) throws IOException {
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
		try (ByteArrayOutputStream output = new ByteArrayOutputStream();
				ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
			writer.setOutput(imageOutput);
			ImageWriteParam params = writer.getDefaultWriteParam();
			if (params.canWriteCompressed()) {
				params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				params.setCompressionQuality(0.94f);
			}
			writer.write(null, new IIOImage(image, null, null), params);
			return output.toByteArray();
		} finally {
			writer.dispose();
		}
	}

	private String sanitize(String value) {
		String normalized = Normalizer.normalize(value, Normalizer.Form.NFKD);
		String sanitized = normalized.replaceAll("[^a-zA-Z0-9._-]", "-");
		return sanitized.replaceAll("-+", "-").replaceAll("^-|-$", "");
	}

	private String contentType(MultipartFile file) {
		return isBlank(file.getContentType()) ? "application/octet-stream" : file.getContentType();
	}

	private String trimTrailingSlash(String value) {
		return value == null ? "" : value.replaceAll("/+$", "");
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
