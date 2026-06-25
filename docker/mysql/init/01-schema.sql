CREATE DATABASE IF NOT EXISTS blog CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE blog;

CREATE TABLE IF NOT EXISTS `user` (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  image VARCHAR(500),
  level INT NOT NULL DEFAULT 0,
  gmt_create DATETIME NOT NULL,
  gmt_modified DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS article (
  id INT PRIMARY KEY AUTO_INCREMENT,
  writer_id INT NOT NULL,
  title VARCHAR(255) NOT NULL,
  content LONGTEXT NOT NULL,
  image TEXT,
  version DECIMAL(10,2) NOT NULL DEFAULT 0.01,
  gmt_create DATETIME NOT NULL,
  gmt_modified DATETIME NOT NULL,
  INDEX idx_article_writer_modified (writer_id, gmt_modified),
  CONSTRAINT fk_article_user FOREIGN KEY (writer_id) REFERENCES `user`(id)
);

CREATE TABLE IF NOT EXISTS tag (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL UNIQUE,
  gmt_create DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS article_tag (
  article_id INT NOT NULL,
  tag_id INT NOT NULL,
  PRIMARY KEY (article_id, tag_id),
  CONSTRAINT fk_article_tag_article FOREIGN KEY (article_id) REFERENCES article(id) ON DELETE CASCADE,
  CONSTRAINT fk_article_tag_tag FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS article_version (
  id INT PRIMARY KEY AUTO_INCREMENT,
  article_id INT NOT NULL,
  version DECIMAL(10,2) NOT NULL,
  title VARCHAR(255) NOT NULL,
  content LONGTEXT NOT NULL,
  tag TEXT,
  gmt_create DATE NOT NULL,
  INDEX idx_article_version (article_id, version),
  CONSTRAINT fk_version_article FOREIGN KEY (article_id) REFERENCES article(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS ai_agent (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  name VARCHAR(50) NOT NULL,
  system_prompt TEXT NOT NULL,
  icon VARCHAR(16),
  gmt_create DATETIME NOT NULL,
  INDEX idx_agent_user (user_id),
  CONSTRAINT fk_agent_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS ai_conversation (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  title VARCHAR(255) NOT NULL,
  agent_id INT,
  gmt_create DATETIME NOT NULL,
  gmt_modified DATETIME NOT NULL,
  INDEX idx_conversation_user_modified (user_id, gmt_modified),
  CONSTRAINT fk_conversation_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE,
  CONSTRAINT fk_conversation_agent FOREIGN KEY (agent_id) REFERENCES ai_agent(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS ai_message (
  id INT PRIMARY KEY AUTO_INCREMENT,
  conversation_id INT NOT NULL,
  role VARCHAR(20) NOT NULL,
  content LONGTEXT NOT NULL,
  gmt_create DATETIME NOT NULL,
  INDEX idx_message_conversation (conversation_id, gmt_create),
  CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES ai_conversation(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS knowledge_base (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(500),
  gmt_create DATETIME NOT NULL,
  INDEX idx_kb_user (user_id),
  CONSTRAINT fk_kb_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS rag_file (
  id INT PRIMARY KEY AUTO_INCREMENT,
  knowledge_base_id INT NOT NULL,
  title VARCHAR(255) NOT NULL,
  file_type VARCHAR(30) NOT NULL,
  r2_key VARCHAR(700),
  status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
  gmt_create DATE NOT NULL,
  hash CHAR(64) NOT NULL,
  version DECIMAL(10,2) NOT NULL DEFAULT 0.01,
  INDEX idx_rag_kb (knowledge_base_id),
  UNIQUE KEY uk_rag_kb_hash (knowledge_base_id, hash),
  CONSTRAINT fk_rag_kb FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base(id) ON DELETE CASCADE
);
