CREATE TABLE AUTHORITY_PERSON (
  authority_person_id INTEGER NOT NULL AUTO_INCREMENT,
  authority_person_uid varchar (100) UNIQUE,
  firstname varchar(255),
  lastname varchar(255),
  created varchar(16),
  PRIMARY KEY (authority_person_id)
);

CREATE TABLE AUTHORITY (
  authority_id INTEGER NOT NULL AUTO_INCREMENT,
  authority_key varchar(255),
  authority_person_id INTEGER,
  authority_value varchar(255),
  PRIMARY KEY (authority_id),
  CONSTRAINT uc_authority_key_value UNIQUE (authority_key, authority_value),
  CONSTRAINT fk_authority_person FOREIGN KEY (authority_person_id)
  REFERENCES AUTHORITY_PERSON(authority_person_id)
);
