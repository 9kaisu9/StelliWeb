package com.stelli.stelli_backend.list;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListRepository extends JpaRepository<StelliList, Long> {

    List<StelliList> findByUserId(Long userId);
}
