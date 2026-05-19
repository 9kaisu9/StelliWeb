package com.stelli.stelli_backend.list;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fields")
@Getter @Setter @NoArgsConstructor
public class Field {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FieldType type;

    private boolean required;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @ElementCollection
    @CollectionTable(name = "field_choices", joinColumns = @JoinColumn(name = "field_id"))
    @Column(name = "choice")
    @BatchSize(size = 50)
    private List<String> choices = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id", nullable = false)
    private StelliList list;
}
