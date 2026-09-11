package com.junhong.somin.notification;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, String> {
	Optional<PushSubscription> findByEndpoint(String endpoint);

	List<PushSubscription> findByUsernameNot(String username);

	List<PushSubscription> findByUsername(String username);

	void deleteByEndpoint(String endpoint);
}
