# Users schema

# --- !Ups

CREATE TABLE User (
  id VARCHAR(255) PRIMARY KEY NOT NULL,
  firstName varchar(32) NOT NULL,
  lastName varchar(32) NOT NULL,
  email varchar(255) NOT NULL,
  PRIMARY KEY (id)
);

create table logininfo (
  id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  providerID VARCHAR(255) NOT NULL,
  providerKey VARCHAR(255) NOT NULL
);

create table userlogininfo (
  userID VARCHAR(255) NOT NULL,
  loginInfoId BIGINT NOT NULL
);

create table passwordinfo (
  hasher VARCHAR(255) NOT NULL,
  password VARCHAR(255) NOT NULL,
  salt VARCHAR(255),
  loginInfoId BIGINT NOT NULL
);

create table authtokeninfo (
  id VARCHAR(258) PRIMARY KEY NOT NULL,
  loginInfoId BIGINT NOT NULL,
  lastUsed DATETIME NOT NULL,
  expiration DATETIME NOT NULL
);


# --- !Downs

DROP TABLE User;
DROP TABLE logininfo;
DROP TABLE userlogininfo;
DROP TABLE passwordinfo;
DROP TABLE authtokeninfo;