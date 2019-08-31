CREATE KEYSPACE IF NOT EXISTS tax WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

USE tax;

CREATE TYPE IF NOT EXISTS contribution (
 type text,
 income DECIMAL,
 base_income DECIMAL,
 rate DECIMAL,
 contribution DECIMAL
);

CREATE TABLE IF NOT EXISTS contributors (
 id TEXT,
 region TEXT,
 registration_date TIMESTAMP,
 yearly_income DECIMAL,
 yearly_contribution DECIMAL,
 PRIMARY KEY ((id, region))
);

CREATE TABLE IF NOT EXISTS contributions (
 contributor_id TEXT,
 year INT,
 month INT,
 income DECIMAL,
 contributions LIST<FROZEN<contribution>>,
 PRIMARY KEY (contributor_id, year, month)
) WITH CLUSTERING ORDER BY (year DESC, month ASC);

