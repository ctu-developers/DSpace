-- SQL query for creating authority person table on oracle.
CREATE TABLE AUTHORITY_PERSON(
  authority_person_id INTEGER,
  authority_person_uid varchar(100) UNIQUE,
  firstname varchar(255),
  lastname varchar(255),
  created varchar(16),
  PRIMARY KEY (authority_person_id)
);

-- SQL query for creating authority person sequence on oracle.
CREATE SEQUENCE AUTHORITY_PERSON_SEQ
  MINVALUE 1
  START WITH 1
  INCREMENT BY 1
  CACHE 20;

-- SQL query for creating authority table on oracle.
CREATE TABLE AUTHORITY (
  authority_id INTEGER,
  authority_key varchar(255),
  authority_person_id INTEGER,
  authority_value varchar(255),
  PRIMARY KEY (authority_id),
  CONSTRAINT uc_authority_key_value UNIQUE (authority_key, authority_value),
  CONSTRAINT fk_authority_person FOREIGN KEY (authority_person_id)
  REFERENCES AUTHORITY_PERSON(authority_person_id)
);

-- SQL query for creating authority table.
CREATE SEQUENCE AUTHORITY_SEQ
  MINVALUE 1
  START WITH 1
  INCREMENT BY 1
  CACHE 20;