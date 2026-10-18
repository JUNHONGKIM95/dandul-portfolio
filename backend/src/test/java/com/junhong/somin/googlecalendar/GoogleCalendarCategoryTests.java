package com.junhong.somin.googlecalendar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.junhong.somin.common.KoreaTime;
import com.junhong.somin.event.DateEvent;
import com.junhong.somin.event.DateEventRepository;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleCalendarCategoryTests {
    final GoogleCalendarConnectionRepository connections = mock(GoogleCalendarConnectionRepository.class);
    final GoogleCalendarEventLinkRepository links = mock(GoogleCalendarEventLinkRepository.class);
    final DateEventRepository events = mock(DateEventRepository.class);
    final HttpClient http = mock(HttpClient.class);
    final ObjectMapper mapper = new ObjectMapper();
    final List<HttpRequest> requests = new ArrayList<>();
    GoogleCalendarService service;
    GoogleCalendarConnection connection;
    boolean moveFails;
    boolean alreadyMoved;
    boolean legacyCalendar;
    boolean importing;
    final java.util.Map<String, String> incomingByCalendar = new java.util.HashMap<>();
    final java.util.Map<String, Boolean> selected = new java.util.HashMap<>(java.util.Map.of("existing-calendar", true, "somin-calendar", true, "junhong-calendar", false));

    @BeforeEach void setup() throws Exception {
        service = new GoogleCalendarService(new GoogleCalendarProperties("client", "secret", "https://example.com/callback",
                "https://www.googleapis.com/auth/calendar", "https://example.com"), connections, links, events);
        ReflectionTestUtils.setField(service, "httpClient", http);
        String token = ReflectionTestUtils.invokeMethod(service, "encrypt", "test-token");
        connection = new GoogleCalendarConnection("junhong", token, token, KoreaTime.now().plusHours(1),
                "https://www.googleapis.com/auth/calendar");
        connection.setGoogleCalendarId("existing-calendar");
        connection.calendarId("somin", "somin-calendar");
        connection.calendarId("junhong", "junhong-calendar");
        when(connections.findForSync("junhong")).thenReturn(Optional.of(connection));
        when(connections.findById("junhong")).thenReturn(Optional.of(connection));
        when(http.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenAnswer(call -> {
                    HttpRequest request = call.getArgument(0);
                    requests.add(request);
                    @SuppressWarnings("unchecked")
                    HttpResponse<String> response = mock(HttpResponse.class);
                    String path = request.uri().getPath();
                    int status = moveFails && path.endsWith("/move") ? 403 : 200;
                    when(response.statusCode()).thenReturn(status);
                    String content = "{\"items\":[]}";
                    if (path.equals("/calendar/v3/calendars") && request.method().equals("POST")) {
                        String name = body(request).path("summary").asText();
                        String category = name.contains("준홍") ? "junhong" : name.contains("소민") ? "somin" : "together";
                        content = "{\"id\":\"new-" + category + "\"}";
                    } else if (path.endsWith("/move")) {
                        content = status == 200 ? "{\"id\":\"moved-event\"}" : "{\"error\":{\"message\":\"move denied\"}}";
                    } else if (request.method().equals("POST")) {
                        content = "{\"id\":\"created-google-event\"}";
                    } else if (path.contains("/users/me/calendarList/") && request.method().equals("PATCH")) {
                        if (body(request).has("selected")) selected.put(path.substring(path.lastIndexOf('/') + 1), body(request).path("selected").asBoolean());
                    } else if (path.contains("/users/me/calendarList/") && request.method().equals("GET")) {
                        content = "{\"selected\":" + selected.getOrDefault(path.substring(path.lastIndexOf('/') + 1), true) + "}";
                    } else if (legacyCalendar && path.endsWith("/legacy-calendar")) {
                        content = "{\"summary\":\"DANDUL\"}";
                    } else if (!incomingByCalendar.isEmpty() && path.endsWith("/events")) {
                        content = incomingByCalendar.getOrDefault(path.split("/")[4], "{\"items\":[]}");
                    } else if (alreadyMoved && path.endsWith("/events")) {
                        content = "{\"items\":[{\"id\":\"recovered-event\"}]}";
                    } else if (importing && path.endsWith("/events")) {
                        String id = path.split("/")[4];
                        content = "{\"items\":[{\"id\":\"native-" + id + "\",\"summary\":\"Google 일정\",\"start\":{\"date\":\"2030-01-02\"},\"end\":{\"date\":\"2030-01-03\"}}]}";
                    }
                    when(response.body()).thenReturn(content);
                    return response;
                });
    }

    DateEvent event(String category) {
        var day = LocalDate.of(2030, 1, 2);
        return new DateEvent(day, day.plusDays(1), LocalTime.of(19, 30), "성수", "저녁 약속", "메모", category, "junhong", "준홍");
    }

    @ParameterizedTest
    @CsvSource({"together,같이,2", "junhong,준홍,9", "somin,소민,4", "legacy,같이,2"})
    void exportsCategoryTitleColorAndMetadataWithoutChangingAppTitle(String category, String label, String color) throws Exception {
        var event = event(category);
        when(events.findAllByOrderByDateAscMeetingTimeAsc()).thenReturn(List.of(event));
        var result = service.syncAll("junhong");
        assertThat(result.created()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        var request = requests.stream().filter(r -> r.method().equals("POST")).findFirst().orElseThrow();
        JsonNode payload = body(request);
        assertThat(payload.path("summary").asText()).isEqualTo("[" + label + "] 저녁 약속");
        assertThat(payload.path("colorId").asText()).isEqualTo(color);
        assertThat(payload.path("description").asText()).contains("일정 구분: " + label, "메모");
        assertThat(payload.path("extendedProperties").path("private").path("dandulEventId").asText()).isEqualTo(event.getId());
        assertThat(payload.path("start").path("dateTime").asText()).isEqualTo("2030-01-02T19:30:00+09:00");
        assertThat(payload.path("end").path("dateTime").asText()).isEqualTo("2030-01-03T20:30:00+09:00");
        assertThat(event.getTitle()).isEqualTo("저녁 약속");
    }

    @Test void categoryChangeMovesSameEventWithoutRecreation() throws Exception {
        var event = event("together");
        var link = new GoogleCalendarEventLink("junhong", event.getId(), "existing-calendar", "existing-event");
        when(events.findAllByOrderByDateAscMeetingTimeAsc()).thenReturn(List.of(event));
        when(links.findByUsernameAndDateEventId("junhong", event.getId())).thenReturn(Optional.of(link));
        when(links.findByUsername("junhong")).thenReturn(List.of(link));
        service.syncAll("junhong");
        event.setCategory("somin");
        service.syncAll("junhong");
        var updates = requests.stream().filter(r -> r.method().equals("PUT")).toList();
        assertThat(updates).hasSize(2);
        assertThat(updates.get(0).uri().getPath()).endsWith("/existing-calendar/events/existing-event");
        assertThat(updates.get(1).uri().getPath()).endsWith("/somin-calendar/events/moved-event");
        assertThat(body(updates.get(1)).path("summary").asText()).isEqualTo("[소민] 저녁 약속");
        assertThat(body(updates.get(1)).path("colorId").asText()).isEqualTo("4");
        assertThat(requests).noneMatch(r -> r.method().equals("DELETE") || (r.method().equals("POST") && !r.uri().getPath().endsWith("/move")));
        assertThat(requests.stream().filter(r -> r.uri().getPath().endsWith("/move"))).hasSize(1);
        assertThat(link.getGoogleCalendarId()).isEqualTo("somin-calendar");
        verify(links, never()).save(any());
    }

    @Test void allDayDateRangeAndLegacyCategoryArePreserved() {
        var event = event(null);
        event.setMeetingTime(null);
        JsonNode payload = ReflectionTestUtils.invokeMethod(service, "eventPayload", event);
        assertThat(payload.path("summary").asText()).isEqualTo("[같이] 저녁 약속");
        assertThat(payload.path("start").path("date").asText()).isEqualTo("2030-01-02");
        assertThat(payload.path("end").path("date").asText()).isEqualTo("2030-01-04");
        assertThat(payload.path("extendedProperties").path("private").path("dandulCategory").asText()).isEqualTo("together");
    }

    @Test void repeatedSyncDoesNotRewriteUnchangedCalendarSettings() {
        selected.put("somin-calendar", false);
        var event = event("together");
        when(events.findAllByOrderByDateAscMeetingTimeAsc()).thenReturn(List.of(event));
        var link = new GoogleCalendarEventLink("junhong", event.getId(), "existing-calendar", "existing-event");
        when(links.findByUsernameAndDateEventId("junhong", event.getId())).thenReturn(Optional.of(link));
        when(links.findByUsername("junhong")).thenReturn(List.of(link));
        service.syncAll("junhong");
        var result = service.syncAll("junhong");
        assertThat(result.calendars()).anySatisfy(c -> { assertThat(c.category()).isEqualTo("somin"); assertThat(c.selected()).isFalse(); });
        assertThat(requests).noneMatch(r -> r.method().equals("PATCH"));
        assertThat(requests.stream().filter(r -> r.method().equals("PUT"))).hasSize(2);
    }

    @Test void createsSeparateCalendarsAndStartsJunhongHidden() throws Exception {
        connection.calendarId("together", null);
        connection.calendarId("junhong", null);
        connection.calendarId("somin", null);
        var result = service.syncAll("junhong");
        assertThat(connection.calendarId("together")).isEqualTo("new-together");
        assertThat(connection.calendarId("somin")).isEqualTo("new-somin");
        assertThat(connection.calendarId("junhong")).isEqualTo("new-junhong");
        assertThat(result.calendars()).anySatisfy(c -> { assertThat(c.name()).isEqualTo("DANDUL (준홍)"); assertThat(c.selected()).isFalse(); });
        assertThat(requests.stream().filter(r -> r.method().equals("POST") && r.uri().getPath().endsWith("/calendars"))).hasSize(3);
        int writes = (int) requests.stream().filter(r -> r.method().equals("PATCH")).count();
        service.syncAll("junhong");
        assertThat(requests.stream().filter(r -> r.method().equals("PATCH"))).hasSize(writes);
    }

    @Test void failedMovePreservesSourceAndDoesNotCreateReplacement() {
        var event = event("somin");
        var link = new GoogleCalendarEventLink("junhong", event.getId(), "existing-calendar", "existing-event");
        when(events.findAllByOrderByDateAscMeetingTimeAsc()).thenReturn(List.of(event));
        when(links.findByUsernameAndDateEventId("junhong", event.getId())).thenReturn(Optional.of(link));
        moveFails = true;
        assertThat(service.syncAll("junhong").failed()).isEqualTo(1);
        assertThat(link.getGoogleCalendarId()).isEqualTo("existing-calendar");
        verify(links, never()).delete(any());
        assertThat(requests).noneMatch(r -> r.method().equals("DELETE") || (r.method().equals("POST") && !r.uri().getPath().endsWith("/move")));
    }

    @Test void retriesRecoverMovedEventWithoutDuplicateCreation() {
        var event = event("somin");
        var link = new GoogleCalendarEventLink("junhong", event.getId(), "existing-calendar", "existing-event");
        when(events.findAllByOrderByDateAscMeetingTimeAsc()).thenReturn(List.of(event));
        when(links.findByUsernameAndDateEventId("junhong", event.getId())).thenReturn(Optional.of(link));
        alreadyMoved = true;
        assertThat(service.syncAll("junhong").updated()).isEqualTo(1);
        assertThat(link.getGoogleCalendarId()).isEqualTo("somin-calendar");
        assertThat(link.getGoogleEventId()).isEqualTo("recovered-event");
        assertThat(requests).noneMatch(r -> r.method().equals("POST") || r.method().equals("DELETE"));
    }

    @Test void migratesLegacyCalendarWithoutDeletingItOrItsEvents() {
        connection.setGoogleCalendarId("legacy-calendar");
        var event = event("together");
        var link = new GoogleCalendarEventLink("junhong", event.getId(), "legacy-calendar", "existing-event");
        when(events.findAllByOrderByDateAscMeetingTimeAsc()).thenReturn(List.of(event));
        when(links.findByUsernameAndDateEventId("junhong", event.getId())).thenReturn(Optional.of(link));
        legacyCalendar = true;
        var result = service.syncAll("junhong");
        assertThat(result.failed()).isZero();
        assertThat(result.updated()).isEqualTo(1);
        assertThat(link.getGoogleCalendarId()).isEqualTo("new-together");
        assertThat(requests).noneMatch(r -> r.method().equals("DELETE"));
    }

    @Test void importsNativeEventsFromAllThreeCalendarsWithTheirCategory() {
        importing = true;
        when(events.save(any())).thenAnswer(call -> call.getArgument(0));
        when(links.save(any())).thenAnswer(call -> call.getArgument(0));
        var result = service.importEvents("junhong");
        assertThat(result.imported()).isEqualTo(3);
        var captured = org.mockito.ArgumentCaptor.forClass(DateEvent.class);
        verify(events, times(3)).save(captured.capture());
        assertThat(captured.getAllValues()).extracting(DateEvent::getCategory).containsExactlyInAnyOrder("together", "somin", "junhong");
    }

    @Test void googleMoveTombstoneDoesNotDeleteAppEvent() {
        var event = event("together");
        var link = new GoogleCalendarEventLink("junhong", event.getId(), "existing-calendar", "existing-event");
        when(links.findByUsername("junhong")).thenReturn(List.of(link));
        when(links.save(any())).thenAnswer(call -> call.getArgument(0));
        when(events.findById(event.getId())).thenReturn(Optional.of(event));
        incomingByCalendar.put("existing-calendar", "{\"items\":[{\"id\":\"existing-event\",\"status\":\"cancelled\"}]}");
        incomingByCalendar.put("somin-calendar", "{\"items\":[{\"id\":\"existing-event\",\"extendedProperties\":{\"private\":{\"dandulEventId\":\"" + event.getId() + "\"}}}]}");
        var result = service.importEvents("junhong");
        assertThat(result.failed()).isZero();
        assertThat(result.deleted()).isZero();
        assertThat(result.updated()).isEqualTo(1);
        assertThat(event.getCategory()).isEqualTo("somin");
        assertThat(link.getGoogleCalendarId()).isEqualTo("somin-calendar");
        verify(events, never()).deleteById(any());
    }

    @Test void genuineGoogleDeletionStillRemovesLinkedAppEvent() {
        var event = event("together");
        var link = new GoogleCalendarEventLink("junhong", event.getId(), "existing-calendar", "existing-event");
        when(links.findByUsername("junhong")).thenReturn(List.of(link));
        when(events.existsById(event.getId())).thenReturn(true);
        incomingByCalendar.put("existing-calendar", "{\"items\":[{\"id\":\"existing-event\",\"status\":\"cancelled\"}]}");
        assertThat(service.importEvents("junhong").deleted()).isEqualTo(1);
        verify(events).deleteById(event.getId());
    }

    @Test void automaticEventSyncRoutesToItsCategoryWithoutChangingVisibility() {
        var event = event("junhong");
        var link = new GoogleCalendarEventLink("junhong", event.getId(), "junhong-calendar", "existing-event");
        when(connections.findAll()).thenReturn(List.of(connection));
        when(events.findById(event.getId())).thenReturn(Optional.of(event));
        when(links.findByUsernameAndDateEventId("junhong", event.getId())).thenReturn(Optional.of(link));
        var result = service.syncEventAcrossConnections(event.getId());
        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        assertThat(requests).anySatisfy(r -> { assertThat(r.method()).isEqualTo("PUT"); assertThat(r.uri().getPath()).endsWith("/junhong-calendar/events/existing-event"); });
        assertThat(requests).noneMatch(r -> r.method().equals("PATCH"));
    }

    @Test void explicitVisibilityChangeIsConfirmedAndPreservedBySubsequentSync() {
        selected.put("junhong-calendar", true);
        var result = service.setCalendarVisibility("junhong", "junhong", false);
        assertThat(result.selected()).isFalse();
        service.syncAll("junhong");
        assertThat(service.calendarVisibility("junhong", "junhong").selected()).isFalse();
        assertThat(requests.stream().filter(r -> r.method().equals("PATCH"))).hasSize(1);
        assertThat(selected.get("somin-calendar")).isTrue();
        assertThat(selected.get("existing-calendar")).isTrue();
    }

    JsonNode body(HttpRequest request) throws Exception {
        var output = new java.io.ByteArrayOutputStream();
        var finished = new CountDownLatch(1);
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
            public void onNext(ByteBuffer buffer) { byte[] bytes = new byte[buffer.remaining()]; buffer.get(bytes); output.writeBytes(bytes); }
            public void onError(Throwable error) { finished.countDown(); }
            public void onComplete() { finished.countDown(); }
        });
        assertThat(finished.await(2, TimeUnit.SECONDS)).isTrue();
        return mapper.readTree(output.toString(StandardCharsets.UTF_8));
    }
}
