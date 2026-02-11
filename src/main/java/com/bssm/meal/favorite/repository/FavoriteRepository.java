package com.bssm.meal.favorite.repository;

import com.bssm.meal.favorite.entity.FavoriteMenu;
import com.bssm.meal.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<FavoriteMenu, Long> {

    List<FavoriteMenu> findByUser(User user);

    // MenuName -> Name 으로 수정 (엔티티의 private String name; 과 일치시킴)
    Optional<FavoriteMenu> findByUserAndName(User user, String name);

    Optional<FavoriteMenu> findByEmailAndName(String email, String name);

    // MenuName -> Name 으로 수정
    Long countByName(String name);

    // MenuName -> Name 으로 수정
    void deleteByUserAndName(User user, String name);

//    List<FavoriteMenu> findByDate(LocalDate date);
    List<FavoriteMenu> findByName(String name);
}