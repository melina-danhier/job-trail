package com.melina.jobtrail.entity;

import com.melina.jobtrail.entity.profile.Profile;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uc_users_email", columnNames = "email")
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private Instant createdAt;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "profile_id", unique = true)
    private Profile profile;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
