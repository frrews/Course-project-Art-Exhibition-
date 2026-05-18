-- UUID → CHAR(36): id в API совпадает с id в БД. Идемпотентно (в т.ч. после частично применённого V3).
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP PROCEDURE IF EXISTS exhibition_drop_fks;
DROP PROCEDURE IF EXISTS exhibition_convert_uuid_to_char;

DELIMITER //
CREATE PROCEDURE exhibition_drop_fks(IN p_table VARCHAR(64))
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE fk_name VARCHAR(255);
    DECLARE cur CURSOR FOR
        SELECT CONSTRAINT_NAME
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND CONSTRAINT_TYPE = 'FOREIGN KEY';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    fk_loop: LOOP
        FETCH cur INTO fk_name;
        IF done THEN
            LEAVE fk_loop;
        END IF;
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;
    CLOSE cur;
END //

CREATE PROCEDURE exhibition_convert_uuid_to_char()
BEGIN
    DECLARE v_type VARCHAR(64);
    DECLARE v_has INT;

    CALL exhibition_drop_fks('user_favorites');
    CALL exhibition_drop_fks('painting_comments');

    SELECT DATA_TYPE INTO v_type
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'id';

    SELECT COUNT(*) INTO v_has
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'id_new';

    IF v_type = 'binary' AND v_has > 0 THEN
        ALTER TABLE users DROP PRIMARY KEY;
        ALTER TABLE users DROP COLUMN id;
        ALTER TABLE users CHANGE COLUMN id_new id CHAR(36) NOT NULL;
        ALTER TABLE users ADD PRIMARY KEY (id);
    ELSEIF v_type = 'binary' THEN
        ALTER TABLE users ADD COLUMN id_new CHAR(36) NULL;
        UPDATE users SET id_new = LOWER(BIN_TO_UUID(id));
        ALTER TABLE users DROP PRIMARY KEY;
        ALTER TABLE users DROP COLUMN id;
        ALTER TABLE users CHANGE COLUMN id_new id CHAR(36) NOT NULL;
        ALTER TABLE users ADD PRIMARY KEY (id);
    END IF;

    SELECT DATA_TYPE INTO v_type
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'paintings' AND COLUMN_NAME = 'id';

    IF v_type = 'binary' THEN
        ALTER TABLE paintings ADD COLUMN id_new CHAR(36) NULL;
        UPDATE paintings SET id_new = LOWER(BIN_TO_UUID(id));
        ALTER TABLE paintings DROP PRIMARY KEY;
        ALTER TABLE paintings DROP COLUMN id;
        ALTER TABLE paintings CHANGE COLUMN id_new id CHAR(36) NOT NULL;
        ALTER TABLE paintings ADD PRIMARY KEY (id);
    END IF;

    SELECT DATA_TYPE INTO v_type
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_favorites' AND COLUMN_NAME = 'user_id';

    IF v_type = 'binary' THEN
        ALTER TABLE user_favorites
            ADD COLUMN user_id_new CHAR(36) NULL,
            ADD COLUMN painting_id_new CHAR(36) NULL;
        UPDATE user_favorites
        SET user_id_new = LOWER(BIN_TO_UUID(user_id)),
            painting_id_new = LOWER(BIN_TO_UUID(painting_id));
        ALTER TABLE user_favorites DROP PRIMARY KEY;
        ALTER TABLE user_favorites DROP COLUMN user_id, DROP COLUMN painting_id;
        ALTER TABLE user_favorites CHANGE COLUMN user_id_new user_id CHAR(36) NOT NULL;
        ALTER TABLE user_favorites CHANGE COLUMN painting_id_new painting_id CHAR(36) NOT NULL;
        ALTER TABLE user_favorites ADD PRIMARY KEY (user_id, painting_id);
    END IF;

    SELECT DATA_TYPE INTO v_type
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'painting_comments' AND COLUMN_NAME = 'painting_id';

    IF v_type = 'binary' THEN
        ALTER TABLE painting_comments
            ADD COLUMN painting_id_new CHAR(36) NULL,
            ADD COLUMN user_id_new CHAR(36) NULL;
        UPDATE painting_comments
        SET painting_id_new = LOWER(BIN_TO_UUID(painting_id)),
            user_id_new = LOWER(BIN_TO_UUID(user_id));
        ALTER TABLE painting_comments DROP COLUMN painting_id, DROP COLUMN user_id;
        ALTER TABLE painting_comments CHANGE COLUMN painting_id_new painting_id CHAR(36) NOT NULL;
        ALTER TABLE painting_comments CHANGE COLUMN user_id_new user_id CHAR(36) NOT NULL;
    END IF;

    SELECT COUNT(*) INTO v_has
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'painting_comments'
      AND CONSTRAINT_NAME = 'fk_comment_painting';

    IF v_has = 0 THEN
        ALTER TABLE painting_comments
            ADD CONSTRAINT fk_comment_painting FOREIGN KEY (painting_id) REFERENCES paintings (id) ON DELETE CASCADE,
            ADD CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
    END IF;

    SELECT DATA_TYPE INTO v_type
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pending_registrations' AND COLUMN_NAME = 'id';

    IF v_type = 'binary' THEN
        ALTER TABLE pending_registrations ADD COLUMN id_new CHAR(36) NULL;
        UPDATE pending_registrations SET id_new = LOWER(BIN_TO_UUID(id));
        ALTER TABLE pending_registrations DROP PRIMARY KEY;
        ALTER TABLE pending_registrations DROP COLUMN id;
        ALTER TABLE pending_registrations CHANGE COLUMN id_new id CHAR(36) NOT NULL;
        ALTER TABLE pending_registrations ADD PRIMARY KEY (id);
    END IF;
END //
DELIMITER ;

CALL exhibition_convert_uuid_to_char();
DROP PROCEDURE IF EXISTS exhibition_convert_uuid_to_char;
DROP PROCEDURE IF EXISTS exhibition_drop_fks;

SET FOREIGN_KEY_CHECKS = 1;
