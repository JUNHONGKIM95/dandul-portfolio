package com.junhong.somin.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final String loginPassword;

	public AuthController(@Value("${app.auth.password:change-me}") String loginPassword) {
		this.loginPassword = loginPassword;
	}

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		if (loginPassword.equals(request.password())) {
			if ("junhong".equals(request.username())) {
				return new LoginResponse("junhong", "준홍");
			}
			if ("somin".equals(request.username())) {
				return new LoginResponse("somin", "소민");
			}
			if ("admin".equals(request.username())) {
				return new LoginResponse("admin", "관리자", "ADMIN");
			}
		}
		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않아요.");
	}

	public record LoginRequest(@NotBlank String username, @NotBlank String password) {
	}

	public record LoginResponse(String username, String nickname, String role) {
		public LoginResponse(String username, String nickname) {
			this(username, nickname, "USER");
		}
	}
}
