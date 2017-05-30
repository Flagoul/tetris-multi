# --- !Ups
CREATE TABLE `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(255) NOT NULL UNIQUE,
  `password` VARCHAR(255) NOT NULL
);


CREATE TABLE `sessions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `uuid` VARCHAR(255) NOT NULL UNIQUE,
  # We set the session to last 1 week
  `expiration` TIMESTAMP NOT NULL,
  `user_id` BIGINT NOT NULL,
  FOREIGN KEY (id) REFERENCES users(id)
);


CREATE TRIGGER set_timestamp BEFORE INSERT ON sessions
FOR EACH ROW
  BEGIN
    SET NEW.expiration = DATE_ADD(UTC_TIMESTAMP, INTERVAL 7 DAY);;
  END;


# --- !Downs
DROP TRIGGER IF EXISTS set_timestamp;
DROP TABLE IF EXISTS `sessions`;
DROP TABLE IF EXISTS `users`;
