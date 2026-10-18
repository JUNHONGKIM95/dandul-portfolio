package com.junhong.somin.googlecalendar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.junhong.somin.common.KoreaTime;
import com.junhong.somin.event.DateEvent;
import com.junhong.somin.event.DateEventRepository;
import com.junhong.somin.googlecalendar.GoogleCalendarDtos.AuthorizationUrlResponse;
import com.junhong.somin.googlecalendar.GoogleCalendarDtos.EventSyncResponse;
import com.junhong.somin.googlecalendar.GoogleCalendarDtos.ImportResponse;
import com.junhong.somin.googlecalendar.GoogleCalendarDtos.StatusResponse;
import com.junhong.somin.googlecalendar.GoogleCalendarDtos.SyncResponse;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GoogleCalendarService {

	private static final String AUTHORIZATION_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
	private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
	private static final String REVOKE_ENDPOINT = "https://oauth2.googleapis.com/revoke";
	private static final String CALENDAR_API = "https://www.googleapis.com/calendar/v3";
	private static final String REQUIRED_CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar";
	private static final String DANDUL_CALENDAR_NAME = "DANDUL";
	private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
	private static final Duration STATE_LIFETIME = Duration.ofMinutes(10);

	private final GoogleCalendarProperties properties;
	private final GoogleCalendarConnectionRepository connectionRepository;
	private final GoogleCalendarEventLinkRepository linkRepository;
	private final DateEventRepository eventRepository;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;
	private final SecureRandom secureRandom;

	public GoogleCalendarService(
			GoogleCalendarProperties properties,
			GoogleCalendarConnectionRepository connectionRepository,
			GoogleCalendarEventLinkRepository linkRepository,
			DateEventRepository eventRepository) {
		this.properties = properties;
		this.connectionRepository = connectionRepository;
		this.linkRepository = linkRepository;
		this.eventRepository = eventRepository;
		this.objectMapper = new ObjectMapper();
		this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
		this.secureRandom = new SecureRandom();
	}

	@Transactional(readOnly = true)
	public StatusResponse status(String username) {
		String owner = normalizeUsername(username);
		Optional<GoogleCalendarConnection> connection = connectionRepository.findById(owner);
		boolean requiresReconnect = connection.isPresent() && !hasRequiredScope(connection.get());
		String message;
		if (!properties.configured()) {
			message = "Google Calendar 서버 설정이 아직 완료되지 않았어요.";
		} else if (requiresReconnect) {
			message = "DANDUL 전용 캘린더 생성을 위해 Google 권한을 갱신해 주세요.";
		} else if (connection.isPresent()) {
			message = connection.get().getGoogleCalendarId() == null
					? "Google 계정이 연결되어 있어요. 동기화하면 DANDUL 캘린더가 만들어져요."
					: "DANDUL 캘린더가 연결되어 있어요.";
		} else {
			message = "Google Calendar를 연결해 주세요.";
		}
		return new StatusResponse(
				properties.configured(),
				connection.isPresent(),
				requiresReconnect,
				owner,
				connection.filter(value -> value.getGoogleCalendarId() != null)
						.map(value -> value.calendarId("somin") != null && value.calendarId("junhong") != null
								? "분류별 DANDUL" : DANDUL_CALENDAR_NAME)
						.orElse(null),
				connection.map(GoogleCalendarConnection::getConnectedAt).orElse(null),
				message);
	}

	public AuthorizationUrlResponse authorizationUrl(String username) {
		requireConfigured();
		String owner = normalizeUsername(username);
		String state = createState(owner);
		String url = AUTHORIZATION_ENDPOINT + "?" + formEncode(List.of(
				new FormValue("client_id", properties.clientId()),
				new FormValue("redirect_uri", properties.redirectUri()),
				new FormValue("response_type", "code"),
				new FormValue("scope", calendarScope()),
				new FormValue("access_type", "offline"),
				new FormValue("include_granted_scopes", "true"),
				new FormValue("prompt", "consent"),
				new FormValue("state", state)));
		return new AuthorizationUrlResponse(url);
	}

	public void completeAuthorization(String code, String state) {
		requireConfigured();
		if (code == null || code.isBlank()) {
			throw new IllegalArgumentException("Google 인증 코드가 없어요.");
		}
		String username = validateState(state);
		JsonNode token = postForm(TOKEN_ENDPOINT, List.of(
				new FormValue("client_id", properties.clientId()),
				new FormValue("client_secret", properties.clientSecret()),
				new FormValue("code", code),
				new FormValue("grant_type", "authorization_code"),
				new FormValue("redirect_uri", properties.redirectUri())));

		String accessToken = requiredText(token, "access_token", "Google 액세스 토큰을 받지 못했어요.");
		String refreshToken = textOrNull(token, "refresh_token");
		String grantedScope = Optional.ofNullable(textOrNull(token, "scope")).orElse(calendarScope());
		long expiresIn = token.path("expires_in").asLong(3600);
		LocalDateTime expiresAt = KoreaTime.now().plusSeconds(Math.max(60, expiresIn));
		Optional<GoogleCalendarConnection> existing = connectionRepository.findById(username);
		if ((refreshToken == null || refreshToken.isBlank()) && existing.isEmpty()) {
			throw new IllegalStateException("Google 갱신 토큰을 받지 못했어요. 권한을 취소한 뒤 다시 연결해 주세요.");
		}

		String encryptedAccessToken = encrypt(accessToken);
		if (existing.isPresent()) {
			existing.get().updateTokens(
					encryptedAccessToken,
					refreshToken == null ? null : encrypt(refreshToken),
					expiresAt,
					grantedScope);
		} else {
			connectionRepository.save(new GoogleCalendarConnection(
					username,
					encryptedAccessToken,
					encrypt(refreshToken),
					expiresAt,
					grantedScope));
		}
	}

	public SyncResponse syncAll(String username) {
		requireConfigured();
		String owner = normalizeUsername(username);
		GoogleCalendarConnection connection = connectionRepository.findForSync(owner)
				.orElseThrow(() -> new IllegalStateException("먼저 Google Calendar를 연결해 주세요."));
		requireCalendarAccess(connection);
		String accessToken = validAccessToken(connection);
		Map<String, String> calendars = ensureDandulCalendars(connection, accessToken);
		List<DateEvent> events = eventRepository.findAllByOrderByDateAscMeetingTimeAsc();
		Set<String> currentEventIds = new HashSet<>();
		List<String> errors = new ArrayList<>();
		int created = 0;
		int updated = 0;
		int deleted = 0;

		for (DateEvent event : events) {
			currentEventIds.add(event.getId());
			try {
				UpsertResult result = upsertEvent(owner, event, calendars.get(category(event)), accessToken);
				if (result == UpsertResult.CREATED) {
					created++;
				} else {
					updated++;
				}
			} catch (RuntimeException ex) {
				errors.add(event.getTitle() + ": " + readableMessage(ex));
			}
		}

		for (GoogleCalendarEventLink link : linkRepository.findByUsername(owner)) {
			if (currentEventIds.contains(link.getDateEventId())) {
				continue;
			}
			try {
				ApiResponse response = calendarRequest(
						"DELETE",
						"/calendars/" + encodePath(link.getGoogleCalendarId())
								+ "/events/" + encodePath(link.getGoogleEventId()),
					null,
						accessToken);
				if (response.success() || response.statusCode() == 404 || response.statusCode() == 410) {
					linkRepository.delete(link);
					deleted++;
				} else {
					throw googleApiError(response);
				}
			} catch (RuntimeException ex) {
				errors.add("삭제된 일정 정리: " + readableMessage(ex));
			}
		}

		List<GoogleCalendarDtos.CalendarVisibility> visibility = new ArrayList<>();
		for (var calendar : calendars.entrySet()) {
			ApiResponse entry = calendarRequest("GET", "/users/me/calendarList/" + encodePath(calendar.getValue()), null, accessToken);
			if (entry.success()) {
				visibility.add(new GoogleCalendarDtos.CalendarVisibility(calendar.getKey(), calendarName(calendar.getKey()), entry.body().path("selected").asBoolean()));
			} else {
				errors.add("캘린더 표시 설정 확인: " + readableMessage(googleApiError(entry)));
			}
		}
		return new SyncResponse(events.size(), created, updated, deleted, errors.size(), errors, visibility);
	}

    public ImportResponse importEvents(String username) {
        requireConfigured();
        String owner = normalizeUsername(username);
        GoogleCalendarConnection connection = connectionRepository.findForSync(owner)
                .orElseThrow(() -> new IllegalStateException("먼저 Google Calendar를 연결해 주세요."));
        requireCalendarAccess(connection);
        String token = validAccessToken(connection);
        Map<String, String> calendars = ensureDandulCalendars(connection, token);
        List<GoogleImportItem> incoming = new ArrayList<>();
        for (var calendar : calendars.entrySet()) {
            String page = null;
            do {
                ApiResponse response = calendarRequest("GET", googleEventsPath(calendar.getValue(), page), null, token);
                if (!response.success()) throw googleApiError(response);
                for (JsonNode item : response.body().path("items")) {
                    incoming.add(new GoogleImportItem(calendar.getKey(), calendar.getValue(), item));
                }
                page = textOrNull(response.body(), "nextPageToken");
            } while (page != null && !page.isBlank());
        }
        // A Google move leaves a cancellation in its old calendar. Handle live copies first.
        incoming.sort(java.util.Comparator.comparing(i -> "cancelled".equals(textOrNull(i.event(), "status"))));
        Map<String, GoogleCalendarEventLink> byPair = new HashMap<>();
        Map<String, GoogleCalendarEventLink> byAppId = new HashMap<>();
        Map<String, List<GoogleCalendarEventLink>> byGoogleId = new HashMap<>();
        for (var link : linkRepository.findByUsername(owner)) {
            byPair.put(link.getGoogleCalendarId() + ":" + link.getGoogleEventId(), link);
            byAppId.put(link.getDateEventId(), link);
            byGoogleId.computeIfAbsent(link.getGoogleEventId(), key -> new ArrayList<>()).add(link);
        }
        Set<String> cancelled = new HashSet<>();
        Map<String, Integer> activeCounts = new HashMap<>();
        for (var item : incoming) {
            String id = textOrNull(item.event(), "id");
            if ("cancelled".equals(textOrNull(item.event(), "status"))) cancelled.add(item.calendarId() + ":" + id);
            else if (id != null) activeCounts.merge(id, 1, Integer::sum);
        }
        List<String> errors = new ArrayList<>();
        int imported = 0, updated = 0, deleted = 0, skipped = 0;
        for (var item : incoming) {
            JsonNode googleEvent = item.event();
            String id = textOrNull(googleEvent, "id");
            if (id == null) { skipped++; continue; }
            String key = item.calendarId() + ":" + id;
            try {
                GoogleCalendarEventLink link = byPair.get(key);
                if ("cancelled".equals(textOrNull(googleEvent, "status"))) {
                    if (link == null || !item.calendarId().equals(link.getGoogleCalendarId()) || !id.equals(link.getGoogleEventId())) {
                        skipped++; continue;
                    }
                    if (eventRepository.existsById(link.getDateEventId())) {
                        eventRepository.deleteById(link.getDateEventId());
                        deleted++;
                    } else skipped++;
                    linkRepository.delete(link);
                    byPair.remove(key);
                    byAppId.remove(link.getDateEventId());
                    continue;
                }
                String appId = googleEvent.path("extendedProperties").path("private").path("dandulEventId").asText(null);
                if (link == null && appId != null) link = byAppId.get(appId);
                if (link == null && activeCounts.getOrDefault(id, 0) == 1) {
                    var candidates = byGoogleId.getOrDefault(id, List.of());
                    if (candidates.size() == 1 && cancelled.contains(candidates.getFirst().getGoogleCalendarId() + ":" + id)) {
                        link = candidates.getFirst();
                    }
                }
                boolean moved = link != null && !item.calendarId().equals(link.getGoogleCalendarId());
                if (moved) {
                    link.moveTo(item.calendarId(), id);
                    linkRepository.save(link);
                    byPair.put(key, link);
                }
                if (appId != null && !appId.isBlank()) {
                    Optional<DateEvent> event = eventRepository.findById(appId);
                    if (event.isPresent() && link == null) {
                        link = linkRepository.save(new GoogleCalendarEventLink(owner, appId, item.calendarId(), id));
                        byPair.put(key, link);
                        byAppId.put(appId, link);
                    }
                    if (moved && event.isPresent()) {
                        event.get().setCategory(item.category());
                        updated++;
                    } else skipped++;
                    continue;
                }
                GoogleEventValues values = googleEventValues(googleEvent);
                if (link == null) {
                    DateEvent event = eventRepository.save(new DateEvent(values.date(), values.endDate(), values.meetingTime(),
                            values.place(), values.title(), values.memo(), item.category(), owner, nickname(owner)));
                    link = linkRepository.save(new GoogleCalendarEventLink(owner, event.getId(), item.calendarId(), id));
                    byPair.put(key, link);
                    byAppId.put(event.getId(), link);
                    imported++;
                } else {
                    Optional<DateEvent> event = eventRepository.findById(link.getDateEventId());
                    if (event.isEmpty()) { linkRepository.delete(link); skipped++; continue; }
                    boolean changed = applyGoogleValues(event.get(), values);
                    if (moved) event.get().setCategory(item.category());
                    if (changed || moved) updated++; else skipped++;
                }
            } catch (RuntimeException ex) {
                errors.add(Optional.ofNullable(textOrNull(googleEvent, "summary")).orElse("Google 일정") + ": " + readableMessage(ex));
            }
        }
        return new ImportResponse(imported, updated, deleted, skipped, errors.size(), errors);
    }

    private record GoogleImportItem(String category, String calendarId, JsonNode event) {}

	public EventSyncResponse syncEventAcrossConnections(String eventId) {
		if (eventId == null || eventId.isBlank()) {
			throw new IllegalArgumentException("동기화할 일정 ID가 없어요.");
		}
		if (!properties.configured()) {
			return new EventSyncResponse(0, 0, 0, 0, 0, 0, List.of());
		}

		Optional<DateEvent> event = eventRepository.findById(eventId);
		List<GoogleCalendarConnection> connections = connectionRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(GoogleCalendarConnection::getUsername)).toList();
		List<String> errors = new ArrayList<>();
		int created = 0;
		int updated = 0;
		int deleted = 0;
		int skipped = 0;

		for (GoogleCalendarConnection connection : connections) {
			String owner = connection.getUsername();
			if (!hasRequiredScope(connection)) {
				skipped++;
				continue;
			}
			try {
				connection = connectionRepository.findForSync(owner).orElseThrow();
				String accessToken = validAccessToken(connection);
				if (event.isPresent()) {
					String calendarId = ensureDandulCalendar(connection, accessToken, category(event.get()));
					UpsertResult result = upsertEvent(owner, event.get(), calendarId, accessToken);
					if (result == UpsertResult.CREATED) {
						created++;
					} else {
						updated++;
					}
					continue;
				}

				Optional<GoogleCalendarEventLink> link =
						linkRepository.findByUsernameAndDateEventId(owner, eventId);
				if (link.isEmpty()) {
					skipped++;
					continue;
				}
				ApiResponse response = calendarRequest(
						"DELETE",
						"/calendars/" + encodePath(link.get().getGoogleCalendarId())
								+ "/events/" + encodePath(link.get().getGoogleEventId()),
						null,
						accessToken);
				if (!response.success() && response.statusCode() != 404 && response.statusCode() != 410) {
					throw googleApiError(response);
				}
				linkRepository.delete(link.get());
				deleted++;
			} catch (RuntimeException ex) {
				errors.add(nickname(owner) + ": " + readableMessage(ex));
			}
		}

		return new EventSyncResponse(
				connections.size(),
				created,
				updated,
				deleted,
				skipped,
				errors.size(),
				errors);
	}

	public GoogleCalendarDtos.CalendarVisibility calendarVisibility(String username, String category) {
		return calendarVisibility(username, category, null);
	}

	public GoogleCalendarDtos.CalendarVisibility setCalendarVisibility(String username, String category, boolean selected) {
		return calendarVisibility(username, category, Boolean.valueOf(selected));
	}

	private GoogleCalendarDtos.CalendarVisibility calendarVisibility(String username, String category, Boolean selected) {
		requireConfigured();
		if (!List.of("together", "somin", "junhong").contains(category)) {
			throw new IllegalArgumentException("일정 구분을 확인해 주세요.");
		}
		GoogleCalendarConnection connection = connectionRepository.findForSync(normalizeUsername(username))
				.orElseThrow(() -> new IllegalStateException("먼저 Google Calendar를 연결해 주세요."));
		requireCalendarAccess(connection);
		String id = connection.calendarId(category);
		if (id == null || id.isBlank()) throw new IllegalStateException("먼저 일정을 동기화해 주세요.");
		String token = validAccessToken(connection);
		String path = "/users/me/calendarList/" + encodePath(id);
		if (selected != null) {
			ApiResponse changed = calendarRequest("PATCH", path,
					objectMapper.createObjectNode().put("selected", selected.booleanValue()), token);
			if (!changed.success()) throw googleApiError(changed);
		}
		ApiResponse entry = calendarRequest("GET", path, null, token);
		if (!entry.success()) throw googleApiError(entry);
		boolean actual = entry.body().path("selected").asBoolean();
		if (selected != null && actual != selected.booleanValue()) {
			throw new IllegalStateException("Google 캘린더 표시 설정이 아직 반영되지 않았어요.");
		}
		return new GoogleCalendarDtos.CalendarVisibility(category, calendarName(category), actual);
	}

	public void disconnect(String username) {
		String owner = normalizeUsername(username);
		connectionRepository.findById(owner).ifPresent(connection -> {
			try {
				postForm(REVOKE_ENDPOINT, List.of(
						new FormValue("token", decrypt(connection.getEncryptedRefreshToken()))));
			} catch (RuntimeException ignored) {
				// Local disconnect must still succeed if Google's revoke endpoint is temporarily unavailable.
			}
			connectionRepository.delete(connection);
		});
	}

	public String frontendRedirect(String result) {
		String base = properties.frontendUrl() == null || properties.frontendUrl().isBlank()
				? "http://localhost:3000"
				: properties.frontendUrl().replaceAll("/+$", "");
		return base + "/?view=calendar&googleCalendar=" + encode(result);
	}

	private String category(DateEvent event) {
        return switch (Objects.toString(event.getCategory(), "")) {
            case "junhong" -> "junhong";
            case "somin" -> "somin";
            default -> "together";
        };
    }

    private String calendarName(String category) {
        return "DANDUL (" + switch (category) {
            case "junhong" -> "준홍";
            case "somin" -> "소민";
            default -> "같이";
        } + ")";
    }

    private Map<String, String> ensureDandulCalendars(GoogleCalendarConnection connection, String token) {
        Map<String, String> result = new java.util.LinkedHashMap<>();
        for (String category : List.of("together", "somin", "junhong")) {
            result.put(category, ensureDandulCalendar(connection, token, category));
        }
        return result;
    }

    private String ensureDandulCalendar(GoogleCalendarConnection connection, String token, String category) {
        String name = calendarName(category);
        String savedId = connection.calendarId(category);
        if (savedId != null && !savedId.isBlank()) {
            ApiResponse saved = calendarRequest("GET", "/calendars/" + encodePath(savedId), null, token);
            if (saved.success() && !("together".equals(category)
                    && DANDUL_CALENDAR_NAME.equals(textOrNull(saved.body(), "summary")))) {
                // Existing preferences, including selected=false, are never overwritten.
                return savedId;
            }
            if (!saved.success() && saved.statusCode() != 404 && saved.statusCode() != 410) throw googleApiError(saved);
            connection.calendarId(category, null);
        }
        String id = findDandulCalendar(token, name);
        if (id != null) {
            connection.calendarId(category, id);
            return ensureDandulCalendar(connection, token, category);
        }
        ObjectNode payload = objectMapper.createObjectNode().put("summary", name)
                .put("description", "DANDUL의 " + name + " 일정").put("timeZone", KOREA_ZONE.getId());
        ApiResponse created = calendarRequest("POST", "/calendars", payload, token);
        if (!created.success()) throw googleApiError(created);
        id = requiredText(created.body(), "id", "Google 캘린더 ID를 받지 못했어요.");
        // Initial preferences only. Sync must not turn a calendar back on after the user hides it.
        ObjectNode style = objectMapper.createObjectNode()
                .put("backgroundColor", switch (category) {
                    case "junhong" -> "#4986e7";
                    case "somin" -> "#f691b2";
                    default -> "#7bd148";
                }).put("foregroundColor", "#000000")
                .put("selected", !("junhong".equals(connection.getUsername()) && "junhong".equals(category)));
        ApiResponse styled = calendarRequest("PATCH", "/users/me/calendarList/" + encodePath(id) + "?colorRgbFormat=true", style, token);
        if (!styled.success()) throw googleApiError(styled);
        connection.calendarId(category, id);
        return id;
    }

    private String findDandulCalendar(String token, String name) {
        String page = null;
        do {
            String path = "/users/me/calendarList?maxResults=250&minAccessRole=owner";
            if (page != null) path += "&pageToken=" + encode(page);
            ApiResponse response = calendarRequest("GET", path, null, token);
            if (!response.success()) throw googleApiError(response);
            for (JsonNode calendar : response.body().path("items")) {
                if (name.equals(textOrNull(calendar, "summary")) || name.equals(textOrNull(calendar, "summaryOverride"))) {
                    return textOrNull(calendar, "id");
                }
            }
            page = textOrNull(response.body(), "nextPageToken");
        } while (page != null && !page.isBlank());
        return null;
    }

	private UpsertResult upsertEvent(
			String username,
			DateEvent event,
			String calendarId,
			String accessToken) {
		ObjectNode payload = eventPayload(event);
		Optional<GoogleCalendarEventLink> existingLink =
				linkRepository.findByUsernameAndDateEventId(username, event.getId());
		if (existingLink.isPresent()
				&& !calendarId.equals(existingLink.get().getGoogleCalendarId())) {
			GoogleCalendarEventLink link = existingLink.get();
			// Recover a previous move that succeeded at Google before the DB transaction committed.
			String movedId = findExistingGoogleEvent(calendarId, event.getId(), accessToken);
			if (movedId == null) {
				ApiResponse move = calendarRequest("POST", "/calendars/" + encodePath(link.getGoogleCalendarId())
						+ "/events/" + encodePath(link.getGoogleEventId()) + "/move?destination=" + encode(calendarId),
						null, accessToken);
				if (move.success()) {
					movedId = requiredText(move.body(), "id", "이동한 Google 일정 ID를 받지 못했어요.");
				} else if (move.statusCode() != 404 && move.statusCode() != 410) {
					// Never delete the source event when a move fails.
					throw googleApiError(move);
				}
			}
			if (movedId != null) {
				link.moveTo(calendarId, movedId);
				linkRepository.saveAndFlush(link);
			} else {
				linkRepository.delete(link);
				linkRepository.flush();
				existingLink = Optional.empty();
			}
		}

		if (existingLink.isPresent()) {
			ApiResponse update = calendarRequest(
					"PUT",
					"/calendars/" + encodePath(calendarId) + "/events/"
							+ encodePath(existingLink.get().getGoogleEventId()),
					payload,
					accessToken);
			if (update.success()) {
				return UpsertResult.UPDATED;
			}
			if (update.statusCode() != 404 && update.statusCode() != 410) {
				throw googleApiError(update);
			}
			linkRepository.delete(existingLink.get());
			linkRepository.flush();
		}

		String foundGoogleEventId = findExistingGoogleEvent(calendarId, event.getId(), accessToken);
		if (foundGoogleEventId != null) {
			ApiResponse update = calendarRequest(
					"PUT",
					"/calendars/" + encodePath(calendarId) + "/events/" + encodePath(foundGoogleEventId),
					payload,
					accessToken);
			if (!update.success()) {
				throw googleApiError(update);
			}
			linkRepository.save(new GoogleCalendarEventLink(
					username, event.getId(), calendarId, foundGoogleEventId));
			return UpsertResult.UPDATED;
		}

		ApiResponse create = calendarRequest(
				"POST",
				"/calendars/" + encodePath(calendarId) + "/events",
				payload,
				accessToken);
		if (!create.success()) {
			throw googleApiError(create);
		}
		String googleEventId = requiredText(create.body(), "id", "Google 일정 ID를 받지 못했어요.");
		linkRepository.save(new GoogleCalendarEventLink(
				username, event.getId(), calendarId, googleEventId));
		return UpsertResult.CREATED;
	}

	private String findExistingGoogleEvent(
			String calendarId,
			String dandulEventId,
			String accessToken) {
		String property = encode("dandulEventId=" + dandulEventId);
		ApiResponse response = calendarRequest(
				"GET",
				"/calendars/" + encodePath(calendarId)
						+ "/events?privateExtendedProperty=" + property
						+ "&showDeleted=false&singleEvents=true&maxResults=1",
				null,
				accessToken);
		if (!response.success()) {
			throw googleApiError(response);
		}
		JsonNode items = response.body().path("items");
		if (!items.isArray() || items.isEmpty()) {
			return null;
		}
		return textOrNull(items.get(0), "id");
	}

	private ObjectNode eventPayload(DateEvent event) {
		ObjectNode payload = objectMapper.createObjectNode();
		String category = switch (Objects.toString(event.getCategory(), "")) {
			case "junhong" -> "junhong";
			case "somin" -> "somin";
			default -> "together";
		};
		String categoryLabel = switch (category) {
			case "junhong" -> "준홍";
			case "somin" -> "소민";
			default -> "같이";
		};
		// Keep one calendar and stable event IDs; category changes update the same event.
		payload.put("summary", "[" + categoryLabel + "] " + event.getTitle());
		payload.put("colorId", switch (category) {
			case "junhong" -> "9";
			case "somin" -> "4";
			default -> "2";
		});
		if (event.getPlace() != null && !event.getPlace().isBlank() && !"장소 미정".equals(event.getPlace())) {
			payload.put("location", event.getPlace());
		}
		String description = event.getMemo() == null || event.getMemo().isBlank()
				? "DANDUL에서 동기화된 일정입니다."
				: event.getMemo().strip() + "\n\nDANDUL에서 동기화된 일정입니다.";
		payload.put("description", "일정 구분: " + categoryLabel + "\n\n" + description);

		LocalDate endDate = event.getEndDate() == null || event.getEndDate().isBefore(event.getDate())
				? event.getDate()
				: event.getEndDate();
		ObjectNode start = payload.putObject("start");
		ObjectNode end = payload.putObject("end");
		if (event.getMeetingTime() == null) {
			start.put("date", event.getDate().toString());
			end.put("date", endDate.plusDays(1).toString());
		} else {
			ZonedDateTime startDateTime = ZonedDateTime.of(event.getDate(), event.getMeetingTime(), KOREA_ZONE);
			ZonedDateTime endDateTime = ZonedDateTime.of(endDate, event.getMeetingTime(), KOREA_ZONE).plusHours(1);
			start.put("dateTime", startDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
			start.put("timeZone", KOREA_ZONE.getId());
			end.put("dateTime", endDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
			end.put("timeZone", KOREA_ZONE.getId());
		}
		payload.putObject("extendedProperties")
				.putObject("private")
				.put("dandulEventId", event.getId())
				.put("dandulCategory", category);
		payload.putObject("reminders").put("useDefault", true);
		return payload;
	}

	private String googleEventsPath(String calendarId, String pageToken) {
		String timeMin = LocalDate.of(2024, 3, 30)
				.atStartOfDay(KOREA_ZONE).toInstant().toString();
		String timeMax = ZonedDateTime.now(KOREA_ZONE)
				.plusYears(5).toInstant().toString();
		StringBuilder path = new StringBuilder()
				.append("/calendars/").append(encodePath(calendarId))
				.append("/events?singleEvents=true&showDeleted=true&maxResults=2500")
				.append("&orderBy=startTime")
				.append("&timeMin=").append(encode(timeMin))
				.append("&timeMax=").append(encode(timeMax));
		if (pageToken != null && !pageToken.isBlank()) {
			path.append("&pageToken=").append(encode(pageToken));
		}
		return path.toString();
	}

	private GoogleEventValues googleEventValues(JsonNode googleEvent) {
		JsonNode start = googleEvent.path("start");
		JsonNode end = googleEvent.path("end");
		String startDate = textOrNull(start, "date");
		LocalDate date;
		LocalDate endDate;
		LocalTime meetingTime;
		if (startDate != null && !startDate.isBlank()) {
			date = LocalDate.parse(startDate);
			String exclusiveEndDate = textOrNull(end, "date");
			endDate = exclusiveEndDate == null || exclusiveEndDate.isBlank()
					? date
					: LocalDate.parse(exclusiveEndDate).minusDays(1);
			meetingTime = null;
		} else {
			ZonedDateTime startDateTime = googleDateTime(start);
			ZonedDateTime endDateTime = googleDateTime(end);
			date = startDateTime.toLocalDate();
			endDate = endDateTime.toLocalDate();
			meetingTime = startDateTime.toLocalTime().withSecond(0).withNano(0);
		}
		if (endDate.isBefore(date)) {
			endDate = date;
		}

		String title = truncate(Optional.ofNullable(textOrNull(googleEvent, "summary"))
				.filter(value -> !value.isBlank())
				.orElse("제목 없음"), 120);
		String location = Optional.ofNullable(textOrNull(googleEvent, "location"))
				.filter(value -> !value.isBlank())
				.map(value -> truncate(value, 255))
				.orElse("장소 미정");
		String description = Optional.ofNullable(textOrNull(googleEvent, "description"))
				.filter(value -> !value.isBlank())
				.map(value -> truncate(value, 2000))
				.orElse(null);
		return new GoogleEventValues(date, endDate, meetingTime, location, title, description);
	}

	private ZonedDateTime googleDateTime(JsonNode dateTimeNode) {
		String value = requiredText(dateTimeNode, "dateTime", "Google 일정의 시간이 올바르지 않아요.");
		try {
			return OffsetDateTime.parse(value).atZoneSameInstant(KOREA_ZONE);
		} catch (DateTimeParseException ignored) {
			String timeZone = Optional.ofNullable(textOrNull(dateTimeNode, "timeZone"))
					.filter(zone -> !zone.isBlank())
					.orElse(KOREA_ZONE.getId());
			return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME)
					.atZone(ZoneId.of(timeZone)).withZoneSameInstant(KOREA_ZONE);
		}
	}

	private boolean applyGoogleValues(DateEvent event, GoogleEventValues values) {
		boolean changed = !Objects.equals(event.getDate(), values.date())
				|| !Objects.equals(event.getEndDate(), values.endDate())
				|| !Objects.equals(event.getMeetingTime(), values.meetingTime())
				|| !Objects.equals(event.getPlace(), values.place())
				|| !Objects.equals(event.getTitle(), values.title())
				|| !Objects.equals(event.getMemo(), values.memo());
		if (!changed) {
			return false;
		}
		event.setDate(values.date());
		event.setEndDate(values.endDate());
		event.setMeetingTime(values.meetingTime());
		event.setPlace(values.place());
		event.setTitle(values.title());
		event.setMemo(values.memo());
		return true;
	}

	private String nickname(String username) {
		return "somin".equals(username) ? "소민" : "준홍";
	}

	private String truncate(String value, int maxLength) {
		return value.length() <= maxLength ? value : value.substring(0, maxLength);
	}

	private String validAccessToken(GoogleCalendarConnection connection) {
		if (connection.getAccessTokenExpiresAt().isAfter(KoreaTime.now().plusMinutes(1))) {
			return decrypt(connection.getEncryptedAccessToken());
		}
		JsonNode token = postForm(TOKEN_ENDPOINT, List.of(
				new FormValue("client_id", properties.clientId()),
				new FormValue("client_secret", properties.clientSecret()),
				new FormValue("refresh_token", decrypt(connection.getEncryptedRefreshToken())),
				new FormValue("grant_type", "refresh_token")));
		String accessToken = requiredText(token, "access_token", "Google 액세스 토큰 갱신에 실패했어요.");
		long expiresIn = token.path("expires_in").asLong(3600);
		connection.updateTokens(
				encrypt(accessToken),
				null,
				KoreaTime.now().plusSeconds(Math.max(60, expiresIn)),
				null);
		return accessToken;
	}

	private ApiResponse calendarRequest(String method, String path, JsonNode body, String accessToken) {
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(CALENDAR_API + path))
				.timeout(Duration.ofSeconds(30))
				.header("Authorization", "Bearer " + accessToken)
				.header("Accept", "application/json");
		if (body != null) {
			builder.header("Content-Type", "application/json; charset=UTF-8")
					.method(method, HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
		} else {
			builder.method(method, HttpRequest.BodyPublishers.noBody());
		}
		HttpResponse<String> response = send(builder.build());
		return new ApiResponse(response.statusCode(), parseJson(response.body()));
	}

	private JsonNode postForm(String endpoint, List<FormValue> values) {
		HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
				.timeout(Duration.ofSeconds(30))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.header("Accept", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(formEncode(values), StandardCharsets.UTF_8))
				.build();
		HttpResponse<String> response = send(request);
		JsonNode body = parseJson(response.body());
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new IllegalStateException(googleErrorMessage(body, response.statusCode()));
		}
		return body;
	}

	private HttpResponse<String> send(HttpRequest request) {
		try {
			return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Google Calendar 요청이 중단되었어요.", ex);
		} catch (Exception ex) {
			throw new IllegalStateException("Google Calendar 서버에 연결하지 못했어요.", ex);
		}
	}

	private JsonNode parseJson(String body) {
		try {
			return body == null || body.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(body);
		} catch (Exception ex) {
			throw new IllegalStateException("Google Calendar 응답을 읽지 못했어요.", ex);
		}
	}

	private IllegalStateException googleApiError(ApiResponse response) {
		return new IllegalStateException(googleErrorMessage(response.body(), response.statusCode()));
	}

	private String googleErrorMessage(JsonNode body, int statusCode) {
		String message = body.path("error").path("message").asText();
		if (message.isBlank()) {
			message = body.path("error_description").asText();
		}
		return message.isBlank()
				? "Google Calendar 요청에 실패했어요. (HTTP " + statusCode + ")"
				: "Google Calendar: " + message;
	}

	private String createState(String username) {
		long expiresAt = System.currentTimeMillis() + STATE_LIFETIME.toMillis();
		String payload = username + "|" + expiresAt + "|" + UUID.randomUUID();
		String encodedPayload = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
		return encodedPayload + "." + sign(encodedPayload);
	}

	private String validateState(String state) {
		if (state == null || !state.contains(".")) {
			throw new IllegalArgumentException("Google 연결 상태값이 올바르지 않아요.");
		}
		String[] parts = state.split("\\.", 2);
		byte[] expected = sign(parts[0]).getBytes(StandardCharsets.UTF_8);
		byte[] actual = parts[1].getBytes(StandardCharsets.UTF_8);
		if (!MessageDigest.isEqual(expected, actual)) {
			throw new IllegalArgumentException("Google 연결 상태값이 일치하지 않아요.");
		}
		try {
			String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
			String[] values = payload.split("\\|", 3);
			if (values.length != 3 || Long.parseLong(values[1]) < System.currentTimeMillis()) {
				throw new IllegalArgumentException("Google 연결 요청이 만료되었어요. 다시 시도해 주세요.");
			}
			return normalizeUsername(values[0]);
		} catch (IllegalArgumentException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new IllegalArgumentException("Google 연결 상태값을 읽지 못했어요.", ex);
		}
	}

	private String sign(String value) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(properties.clientSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return Base64.getUrlEncoder().withoutPadding()
					.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Google 연결 보안값을 만들지 못했어요.", ex);
		}
	}

	private String encrypt(String value) {
		try {
			byte[] iv = new byte[12];
			secureRandom.nextBytes(iv);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, encryptionKey(), new GCMParameterSpec(128, iv));
			byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
			ByteBuffer output = ByteBuffer.allocate(iv.length + encrypted.length);
			output.put(iv).put(encrypted);
			return Base64.getUrlEncoder().withoutPadding().encodeToString(output.array());
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Google 연결 정보를 암호화하지 못했어요.", ex);
		}
	}

	private String decrypt(String value) {
		try {
			byte[] input = Base64.getUrlDecoder().decode(value);
			ByteBuffer buffer = ByteBuffer.wrap(input);
			byte[] iv = new byte[12];
			buffer.get(iv);
			byte[] encrypted = new byte[buffer.remaining()];
			buffer.get(encrypted);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), new GCMParameterSpec(128, iv));
			return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
		} catch (Exception ex) {
			throw new IllegalStateException("저장된 Google 연결 정보를 읽지 못했어요. 다시 연결해 주세요.", ex);
		}
	}

	private SecretKeySpec encryptionKey() {
		try {
			byte[] key = MessageDigest.getInstance("SHA-256")
					.digest(properties.clientSecret().getBytes(StandardCharsets.UTF_8));
			return new SecretKeySpec(key, "AES");
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Google 연결 암호화 키를 만들지 못했어요.", ex);
		}
	}

	private String normalizeUsername(String username) {
		if (!"junhong".equals(username) && !"somin".equals(username)) {
			throw new IllegalArgumentException("Google Calendar를 연결할 사용자가 올바르지 않아요.");
		}
		return username;
	}

	private void requireConfigured() {
		if (!properties.configured()) {
			throw new IllegalStateException("Google Calendar 서버 환경변수가 설정되지 않았어요.");
		}
	}

	private void requireCalendarAccess(GoogleCalendarConnection connection) {
		if (!hasRequiredScope(connection)) {
			throw new IllegalStateException(
					"DANDUL 전용 캘린더 생성 권한이 필요해요. CALENDAR 화면에서 '권한 갱신'을 눌러 주세요.");
		}
	}

	private boolean hasRequiredScope(GoogleCalendarConnection connection) {
		return hasScope(connection.getGrantedScope(), REQUIRED_CALENDAR_SCOPE);
	}

	private boolean hasScope(String scopes, String requiredScope) {
		if (scopes == null || scopes.isBlank()) {
			return false;
		}
		return List.of(scopes.trim().split("\\s+")).contains(requiredScope);
	}

	private String calendarScope() {
		String configuredScope = properties.scope();
		return hasScope(configuredScope, REQUIRED_CALENDAR_SCOPE)
				? configuredScope
				: REQUIRED_CALENDAR_SCOPE;
	}

	private String requiredText(JsonNode node, String field, String message) {
		String value = textOrNull(node, field);
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(message);
		}
		return value;
	}

	private String textOrNull(JsonNode node, String field) {
		JsonNode value = node == null ? null : node.get(field);
		return value == null || value.isNull() ? null : value.asText();
	}

	private String formEncode(List<FormValue> values) {
		return values.stream()
				.map(value -> encode(value.name()) + "=" + encode(value.value()))
				.reduce((first, second) -> first + "&" + second)
				.orElse("");
	}

	private String encodePath(String value) {
		return encode(value).replace("+", "%20");
	}

	private String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private String readableMessage(Throwable throwable) {
		return throwable.getMessage() == null || throwable.getMessage().isBlank()
				? throwable.getClass().getSimpleName()
				: throwable.getMessage();
	}

	private enum UpsertResult {
		CREATED,
		UPDATED
	}

	private record FormValue(String name, String value) {
	}

	private record ApiResponse(int statusCode, JsonNode body) {
		boolean success() {
			return statusCode >= 200 && statusCode < 300;
		}
	}

	private record GoogleEventValues(
			LocalDate date,
			LocalDate endDate,
			LocalTime meetingTime,
			String place,
			String title,
			String memo) {
	}
}
