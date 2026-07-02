CREATE TABLE chat_rooms (
                            id CHAR(36) PRIMARY KEY,
                            name VARCHAR(200),
                            type ENUM('DIRECT','EVENT') NOT NULL,
                            event_id CHAR(36),
                            participant_one_id CHAR(36),
                            participant_two_id CHAR(36),
                            created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                            updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                            INDEX idx_room_type (type),
                            INDEX idx_event_id (event_id),
                            INDEX idx_participants (participant_one_id, participant_two_id),
                            CONSTRAINT fk_chat_room_event FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
                            CONSTRAINT fk_chat_room_participant_one FOREIGN KEY (participant_one_id) REFERENCES users (id) ON DELETE CASCADE,
                            CONSTRAINT fk_chat_room_participant_two FOREIGN KEY (participant_two_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE chat_messages (
                               id CHAR(36) PRIMARY KEY,
                               chat_room_id CHAR(36),
                               sender_id CHAR(36) NOT NULL,
                               recipient_id CHAR(36) NOT NULL,
                               content TEXT NOT NULL,
                               read_at DATETIME(6) NULL,
                               created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                               updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                               INDEX idx_chat_room (chat_room_id),
                               INDEX idx_chat_sender (sender_id),
                               INDEX idx_chat_recipient (recipient_id),
                               INDEX idx_chat_conversation_created (sender_id, recipient_id, created_at),
                               INDEX idx_chat_recipient_unread (recipient_id, read_at),
                               CONSTRAINT fk_chat_message_room FOREIGN KEY (chat_room_id) REFERENCES chat_rooms (id) ON DELETE CASCADE,
                               CONSTRAINT fk_chat_sender FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE CASCADE,
                               CONSTRAINT fk_chat_recipient FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
