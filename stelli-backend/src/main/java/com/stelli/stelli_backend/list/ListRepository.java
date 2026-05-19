package com.stelli.stelli_backend.list;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ListRepository extends JpaRepository<StelliList, Long> {

    @Query("SELECT DISTINCT l FROM StelliList l LEFT JOIN FETCH l.fields WHERE l.userId = :userId")
    List<StelliList> findByUserId(@Param("userId") Long userId);
}
