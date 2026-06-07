package com.stelli.stelli_backend.entry;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

public class EntryRepositoryImpl implements EntryQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Entry> search(Long listId, EntrySearchCriteria criteria) {
        var hasFilter = hasText(criteria.filterField());
        var hasSearch = hasText(criteria.q());
        var hasSort = hasText(criteria.sortField());

        var sql = new StringBuilder("SELECT * FROM entries WHERE list_id = :listId");
        if (hasFilter) {
            sql.append(" AND CAST(json_extract(field_values, :filterPath) AS TEXT) = :filterValue");
        }
        if (hasSearch) {
            sql.append(" AND lower(search_text) LIKE lower(:q)");
        }
        if (hasSort) {
            sql.append(" ORDER BY json_extract(field_values, :sortPath) ").append(direction(criteria.sortDir()));
        }

        var query = entityManager.createNativeQuery(sql.toString(), Entry.class);
        query.setParameter("listId", listId);
        if (hasFilter) {
            query.setParameter("filterPath", "$." + criteria.filterField());
            query.setParameter("filterValue", criteria.filterValue());
        }
        if (hasSearch) {
            query.setParameter("q", "%" + criteria.q() + "%");
        }
        if (hasSort) {
            query.setParameter("sortPath", "$." + criteria.sortField());
        }

        @SuppressWarnings("unchecked")
        List<Entry> results = query.getResultList();
        return results;
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static String direction(String sortDir) {
        return "desc".equalsIgnoreCase(sortDir) ? "DESC" : "ASC";
    }
}
