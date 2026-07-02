package com.melina.jobtrail.entity.profile;

import com.melina.jobtrail.util.LanguageLevel;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "profile_languages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileLanguage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LanguageLevel level;

    @ManyToOne(optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;
}
