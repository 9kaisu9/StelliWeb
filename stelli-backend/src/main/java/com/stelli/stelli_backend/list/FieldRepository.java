package com.stelli.stelli_backend.list;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FieldRepository extends JpaRepository<Field, Long> {

    @Query("SELECT f FROM Field f WHERE f.list.id = :listId ORDER BY f.displayOrder ASC")
    List<Field> findByListId(@Param("listId") Long listId);
}
