CREATE TABLE participations (
                                id VARCHAR(36) NOT NULL,
                                user_id VARCHAR(36) NOT NULL,
                                event_id VARCHAR(36) NOT NULL,
                                status VARCHAR(20) NOT NULL,
                                joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                PRIMARY KEY (id),

    -- User və Event cədvəlləri ilə əlaqələr
                                CONSTRAINT fk_participation_user FOREIGN KEY (user_id)
                                    REFERENCES users(id) ON DELETE CASCADE,

                                CONSTRAINT fk_participation_event FOREIGN KEY (event_id)
                                    REFERENCES events(id) ON DELETE CASCADE,

    -- Eyni istifadəçinin eyni tədbirə birdən çox qatılmasının qarşısını almaq üçün
                                CONSTRAINT uq_user_event UNIQUE (user_id, event_id)
);

-- Performans üçün indekslər
CREATE INDEX idx_participation_user ON participations(user_id);
CREATE INDEX idx_participation_event ON participations(event_id);