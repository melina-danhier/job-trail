package com.melina.jobtrail.entity.profile;

import com.melina.jobtrail.util.SkillLevel;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "profile_skills")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SkillLevel level;

    @Column(name = "is_main_skill", nullable = false)
    private boolean mainSkill;

    @ManyToOne(optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;
}
