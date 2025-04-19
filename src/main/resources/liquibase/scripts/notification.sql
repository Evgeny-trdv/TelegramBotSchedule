-- liquibase formatted sql

--changeset etaradaev:1
CREATE TABLE notification (
    id SERIAL,
    chat_id SERIAL,
    message TEXT,
    date TIMESTAMP
    )

--changeset etaradaev:2
ALTER TABLE notification ADD sent BOOLEAN