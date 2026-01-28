-- Script de inicialização do banco de dados
-- Executado automaticamente quando o MySQL sobe pela primeira vez
-- Cria usuários de teste para testes E2E

-- Aguarda a tabela usuarios existir (criada pelo Hibernate)
-- Se a tabela não existir, este INSERT será ignorado na primeira execução
-- O script será útil após a aplicação criar as tabelas

-- Usuários de teste pré-cadastrados
-- Senha: 123456 (hash BCrypt gerado com strength 10)
-- Hash: $2a$10$6.NhjLVGWREJzlRet9xUzOXpwhxJ91LN55d.Jxqs9m/zkdnqHC29G

-- Nota: Este script é executado ANTES da aplicação iniciar,
-- então as tabelas ainda não existem. Os usuários serão inseridos
-- via script de teste ou manualmente após a aplicação subir.

-- Para referência, os comandos de inserção seriam:
-- INSERT INTO usuarios (email, senha_hash, criado_em) VALUES
--   ('teste@email.com', '$2a$10$6.NhjLVGWREJzlRet9xUzOXpwhxJ91LN55d.Jxqs9m/zkdnqHC29G', NOW()),
--   ('outro@email.com', '$2a$10$6.NhjLVGWREJzlRet9xUzOXpwhxJ91LN55d.Jxqs9m/zkdnqHC29G', NOW())
-- ON DUPLICATE KEY UPDATE email=email;
