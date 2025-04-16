-- liquibase formatted sql

--changeset etaradaev:1
CREATE TABLE notification (
    id SERIAL,
    chat_id SERIAL,
    message TEXT,
    date TIMESTAMP
    )