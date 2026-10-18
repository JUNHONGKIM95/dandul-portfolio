package com.junhong.somin.memo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/memos")
public class DailyMemoController {
    private final DailyMemoService service;
    public DailyMemoController(DailyMemoService service) { this.service = service; }
    public record MemoRequest(@NotBlank String createdBy, @NotBlank @Size(max = 500) String content) {}
    @GetMapping
    public DailyMemoService.TodayResponse today() { return service.today(); }
    @PostMapping
    public DailyMemoService.TodayResponse add(@Valid @RequestBody MemoRequest request) {
        return service.add(request.createdBy(), request.content());
    }
}
