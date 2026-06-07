package com.stelli.stelli_backend.entry;

import java.util.List;

public interface EntryQueryRepository {
    List<Entry> search(Long listId, EntrySearchCriteria criteria);
}
