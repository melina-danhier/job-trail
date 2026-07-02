package com.melina.jobtrail.entity.profile;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "profile_projects")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 2000)
    private String description;

    @ElementCollection
    @CollectionTable(name = "profile_project_technologies", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "technology", length = 100)
    @Builder.Default
    private Set<String> technologies = new HashSet<>();

    @ManyToOne(optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;
}
