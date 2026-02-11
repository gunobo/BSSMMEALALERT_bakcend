package com.bssm.meal.admin.repository;

import com.bssm.meal.admin.domain.AppDownloadStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppDownloadRepository extends JpaRepository<AppDownloadStats, String> {
}