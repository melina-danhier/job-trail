package com.melina.jobtrail.entity.profile;

import com.melina.jobtrail.util.ExperienceLevel;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String targetRole;

    @Column(nullable = false, length = 100)
    private String locationPreference;

    @Column(nullable = false, length = 100)
    private String availability;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExperienceLevel experienceLevel;

    @Column(length = 2000)
    private String summary;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProfileSkill> skills = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProfileLanguage> languages = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProfileProject> projects = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "profile_preferred_roles", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "role")
    @Builder.Default
    private Set<String> preferredRoles = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "profile_avoid_keywords", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "keyword")
    @Builder.Default
    private Set<String> avoidKeywords = new HashSet<>();

    public void setSkills(List<ProfileSkill> skills) {
        this.skills = skills == null ? new ArrayList<>() : new ArrayList<>(skills);
        this.skills.forEach(skill -> skill.setProfile(this));
    }

    public void setLanguages(List<ProfileLanguage> languages) {
        this.languages = languages == null ? new ArrayList<>() : new ArrayList<>(languages);
        this.languages.forEach(language -> language.setProfile(this));
    }

    public void setProjects(List<ProfileProject> projects) {
        this.projects = projects == null ? new ArrayList<>() : new ArrayList<>(projects);
        this.projects.forEach(project -> project.setProfile(this));
    }

    public void setPreferredRoles(Set<String> preferredRoles) {
        this.preferredRoles = preferredRoles == null ? new HashSet<>() : new HashSet<>(preferredRoles);
    }

    public void setAvoidKeywords(Set<String> avoidKeywords) {
        this.avoidKeywords = avoidKeywords == null ? new HashSet<>() : new HashSet<>(avoidKeywords);
    }
}
