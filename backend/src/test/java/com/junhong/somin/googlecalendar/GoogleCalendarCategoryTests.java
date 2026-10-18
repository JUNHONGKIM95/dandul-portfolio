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

    @BeforeEach void setup() throws Exception {
        service = new GoogleCalendarService(new GoogleCalendarProperties("client", "secret", "https://example.com/callback",
                "https://www.googleapis.com/auth/calendar", "https://example.com"), connections, links, events);
        ReflectionTestUtils.setField(service, "httpClient", http);
        String token = ReflectionTestUtils.invokeMethod(service, "encrypt", "test-token");
        var connection = new GoogleCalendarConnection("junhong", token, token, KoreaTime.now().plusHours(1),
                "https://www.googleapis.com/auth/calendar");
        connection.setGoogleCalendarId("existing-calendar");
        when(connections.findById("junhong")).thenReturn(Optional.of(connection));
        when(http.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenAnswer(call -> {
                    HttpRequest request = call.getArgument(0);
                    requests.add(request);
                    @SuppressWarnings("unchecked")
                    HttpResponse<String> response = mock(HttpResponse.class);
                    when(response.statusCode()).thenReturn(200);
                    when(response.body()).thenReturn(request.method().equals("POST") ? "{\"id\":\"created-google-event\"}" : "{\"items\":[]}");
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

    @Test void categoryChangeUpdatesSameEventWithoutRecreation() throws Exception {
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
        assertThat(updates).allSatisfy(r -> assertThat(r.uri().getPath()).endsWith("/existing-calendar/events/existing-event"));
        assertThat(body(updates.get(1)).path("summary").asText()).isEqualTo("[소민] 저녁 약속");
        assertThat(body(updates.get(1)).path("colorId").asText()).isEqualTo("4");
        assertThat(requests).noneMatch(r -> r.method().equals("POST") || r.method().equals("DELETE"));
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
