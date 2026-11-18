-- ============================================================
--  Help Desk Ticketing System – Database Schema
--  Run this script once to initialize the database.
-- ============================================================

-- (Optional for local MySQL: create and select database)
-- CREATE DATABASE IF NOT EXISTS helpdesk CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE helpdesk;

-- ── users ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    user_id       INT          NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('USER','AGENT','ADMIN') NOT NULL DEFAULT 'USER',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB;

-- ── agents ────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS agents (
    agent_id  INT     NOT NULL AUTO_INCREMENT,
    user_id   INT     NOT NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (agent_id),
    CONSTRAINT fk_agents_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ── tickets ───────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tickets (
    ticket_id   INT          NOT NULL AUTO_INCREMENT,
    user_id     INT          NOT NULL,
    agent_id    INT                   DEFAULT NULL,
    title       VARCHAR(200) NOT NULL,
    description TEXT         NOT NULL,
    category    ENUM('Technical','Network','Hardware','Software','Account/Login') NOT NULL,
    priority    ENUM('Low','Medium','High','Critical') NOT NULL DEFAULT 'Low',
    status      ENUM('OPEN','ASSIGNED','IN_PROGRESS','RESOLVED') NOT NULL DEFAULT 'OPEN',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    resolved_at DATETIME              NULL DEFAULT NULL,
    PRIMARY KEY (ticket_id),
    CONSTRAINT fk_tickets_user
        FOREIGN KEY (user_id)  REFERENCES users(user_id)  ON DELETE RESTRICT,
        CONSTRAINT fk_tickets_agent
        FOREIGN KEY (agent_id) REFERENCES agents(agent_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ── ticket_notes ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ticket_notes (
    note_id    INT       NOT NULL AUTO_INCREMENT,
    ticket_id  INT       NOT NULL,
    agent_id   INT                DEFAULT NULL,
    note       TEXT      NOT NULL,
    created_at DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (note_id),
    CONSTRAINT fk_notes_ticket
        FOREIGN KEY (ticket_id) REFERENCES tickets(ticket_id) ON DELETE CASCADE,
    CONSTRAINT fk_notes_agent
        FOREIGN KEY (agent_id)  REFERENCES agents(agent_id)  ON DELETE SET NULL
) ENGINE=InnoDB;

-- ============================================================
--  Sample / seed data
--  All passwords are BCrypt hash of "admin123"
-- ============================================================

INSERT IGNORE INTO users (name, email, password_hash, role) VALUES
    ('Admin User',  'admin@helpdesk.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN'),
    ('Agent One',   'agent1@helpdesk.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AGENT'),
    ('Agent Two',   'agent2@helpdesk.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AGENT'),
    ('User One',    'user1@helpdesk.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER'),
    ('User Two',    'user2@helpdesk.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER');

-- Register the two agent users in the agents table
INSERT IGNORE INTO agents (user_id, available)
    SELECT u.user_id, TRUE FROM users u 
    WHERE u.email IN ('agent1@helpdesk.com','agent2@helpdesk.com')
      AND NOT EXISTS (SELECT 1 FROM agents a WHERE a.user_id = u.user_id);

-- Sample tickets (submitted by user1 and user2)
INSERT INTO tickets (user_id, agent_id, title, description, category, priority, status)
SELECT 
    u.user_id,
    a.agent_id,
    'Cannot connect to VPN',
    'I am unable to establish a VPN connection from home since the last software update.',
    'Network', 'High', 'ASSIGNED'
FROM users u
LEFT JOIN users u_agent ON u_agent.email = 'agent1@helpdesk.com'
LEFT JOIN agents a ON a.user_id = u_agent.user_id
WHERE u.email = 'user1@helpdesk.com'
  AND NOT EXISTS (SELECT 1 FROM tickets WHERE title = 'Cannot connect to VPN');

INSERT INTO tickets (user_id, agent_id, title, description, category, priority, status)
SELECT 
    u.user_id,
    NULL,
    'Printer not responding',
    'The office printer on the 3rd floor is offline and not accepting print jobs.',
    'Hardware', 'Medium', 'OPEN'
FROM users u
WHERE u.email = 'user1@helpdesk.com'
  AND NOT EXISTS (SELECT 1 FROM tickets WHERE title = 'Printer not responding');

INSERT INTO tickets (user_id, agent_id, title, description, category, priority, status)
SELECT 
    u.user_id,
    a.agent_id,
    'Password reset required',
    'My account is locked after multiple failed login attempts. Please reset my password.',
    'Account/Login', 'Critical', 'IN_PROGRESS'
FROM users u
LEFT JOIN users u_agent ON u_agent.email = 'agent2@helpdesk.com'
LEFT JOIN agents a ON a.user_id = u_agent.user_id
WHERE u.email = 'user2@helpdesk.com'
  AND NOT EXISTS (SELECT 1 FROM tickets WHERE title = 'Password reset required');
