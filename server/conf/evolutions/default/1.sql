# User schema

# --- !Ups
create table `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(255) NOT NULL UNIQUE,
  `password` VARCHAR(255)
);

# --- !Downs
drop table `users`;
