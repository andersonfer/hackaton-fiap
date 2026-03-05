-- Script de criacao do banco de dados - FIAP X Video Processor
-- Executado automaticamente quando o MySQL sobe pela primeira vez via Docker Compose

CREATE DATABASE IF NOT EXISTS fiapx CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE fiapx;

CREATE TABLE IF NOT EXISTS usuarios (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    email      VARCHAR(255) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    criado_em  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_usuarios_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS videos (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    usuario_id       BIGINT       NOT NULL,
    nome_original    VARCHAR(255) NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE',
    caminho_arquivo  VARCHAR(500),
    caminho_zip      VARCHAR(500),
    mensagem_erro    TEXT,
    criado_em        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    atualizado_em    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_videos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
