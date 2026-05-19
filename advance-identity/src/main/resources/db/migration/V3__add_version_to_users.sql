-- Колонка для оптимистичной блокировки (JPA @Version)
ALTER TABLE users ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
