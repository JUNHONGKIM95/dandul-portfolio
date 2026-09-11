package com.junhong.somin.foodie;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodiePlaceRepository extends JpaRepository<FoodiePlace, String> {
	List<FoodiePlace> findAllByOrderByVisitDateDescUpdatedAtDesc();
}
