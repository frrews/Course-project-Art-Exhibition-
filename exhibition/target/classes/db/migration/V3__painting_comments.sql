-- Комментарии к картинам: связь по painting_id (UUID), не по названию (названия могут повторяться).
CREATE TABLE IF NOT EXISTS painting_comments (
    id CHAR(36) NOT NULL PRIMARY KEY,
    painting_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_comment_painting FOREIGN KEY (painting_id) REFERENCES paintings(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    KEY idx_comments_painting (painting_id),
    KEY idx_comments_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
