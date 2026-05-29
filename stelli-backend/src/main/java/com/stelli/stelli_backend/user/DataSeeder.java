package com.stelli.stelli_backend.user;

import com.stelli.stelli_backend.list.CreateListRequest;
import com.stelli.stelli_backend.list.FieldRequest;
import com.stelli.stelli_backend.list.FieldType;
import com.stelli.stelli_backend.list.ListRepository;
import com.stelli.stelli_backend.list.ListService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    public static final long DEFAULT_USER_ID = 1L;

    private final UserRepository userRepository;
    private final ListRepository listRepository;
    private final ListService listService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (userRepository.count() == 0) {
            userRepository.save(new User());
        }
        if (listRepository.count() == 0) {
            seedTemplates();
        }
    }

    private void seedTemplates() {
        listService.create(new CreateListRequest("Restaurants", "Places I want to visit", "🍽", List.of(
            field("Name",              FieldType.TEXT,        true,  0),
            field("Cuisine Type",      FieldType.OPTION,      false, 1, "Italian", "Japanese", "Mexican", "American", "Chinese", "Indian", "Thai", "French"),
            field("Rating",            FieldType.RATING,      false, 2),
            field("Price Range",       FieldType.OPTION,      false, 3, "$", "$$", "$$$", "$$$$"),
            field("Location",          FieldType.LOCATION,    false, 4),
            field("Notes",             FieldType.TEXT,        false, 5),
            field("Has Usable Toilet", FieldType.BOOLEAN,     false, 6)
        )));

        listService.create(new CreateListRequest("Movies", "Films to watch", "🎬", List.of(
            field("Title",    FieldType.TEXT,   true,  0),
            field("Director", FieldType.TEXT,   false, 1),
            field("Genre",    FieldType.OPTION, false, 2, "Action", "Comedy", "Drama", "Horror", "Sci-Fi", "Documentary", "Thriller", "Romance"),
            field("Rating",   FieldType.RATING, false, 3),
            field("Year",     FieldType.NUMBER, false, 4),
            field("Notes",    FieldType.TEXT,   false, 5)
        )));

        listService.create(new CreateListRequest("Journal", "Daily reflections", "📓", List.of(
            field("Date",  FieldType.DATE,   true,  0),
            field("Title", FieldType.TEXT,   true,  1),
            field("Mood",  FieldType.RATING, false, 2),
            field("Entry", FieldType.TEXT,   false, 3)
        )));

        listService.create(new CreateListRequest("Travel Sites", "Places to explore", "✈️", List.of(
            field("Name",         FieldType.TEXT,     true,  0),
            field("Country",      FieldType.TEXT,     false, 1),
            field("City",         FieldType.TEXT,     false, 2),
            field("Rating",       FieldType.RATING,   false, 3),
            field("Visited Date", FieldType.DATE,     false, 4),
            field("Notes",        FieldType.TEXT,     false, 5),
            field("Location",     FieldType.LOCATION, false, 6)
        )));
    }

    private static FieldRequest field(String name, FieldType type, boolean required, int order, String... choices) {
        return new FieldRequest(name, type, required, order, List.of(choices));
    }
}
