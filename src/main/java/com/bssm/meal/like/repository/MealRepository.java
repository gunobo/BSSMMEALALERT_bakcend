package com.bssm.meal.like.repository;

import com.bssm.meal.like.entity.MealLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MealRepository extends JpaRepository<MealLike, Long> {
    Optional<MealLike> findByMealDate(String mealDate);
}