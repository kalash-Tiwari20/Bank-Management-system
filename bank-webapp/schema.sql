CREATE DATABASE IF NOT EXISTS bankdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bankdb;

CREATE TABLE users (
  user_id INT AUTO_INCREMENT PRIMARY KEY,
  first_name VARCHAR(100) NOT NULL,
  last_name VARCHAR(100) NOT NULL,
  father_name VARCHAR(150),
  mother_name VARCHAR(150),
  gender VARCHAR(20),
  age INT,
  phone VARCHAR(15),
  aadhaar CHAR(12),
  pan CHAR(10),
  email VARCHAR(150),
  password_hash VARCHAR(1024),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE accounts (
  account_id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  account_number VARCHAR(40) NOT NULL UNIQUE,
  account_type VARCHAR(20) NOT NULL,
  balance DECIMAL(15,2) NOT NULL DEFAULT 0,
  opened_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  status VARCHAR(20) DEFAULT 'ACTIVE',
  CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE transactions (
  transaction_id INT AUTO_INCREMENT PRIMARY KEY,
  from_account_id INT NULL,
  to_account_id INT NULL,
  amount DECIMAL(15,2) NOT NULL,
  txn_type VARCHAR(30),
  description VARCHAR(255),
  txn_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  status VARCHAR(30),
  post_balance DECIMAL(15,2),
  CONSTRAINT fk_tx_from_acc FOREIGN KEY (from_account_id) REFERENCES accounts(account_id) ON DELETE SET NULL,
  CONSTRAINT fk_tx_to_acc FOREIGN KEY (to_account_id) REFERENCES accounts(account_id) ON DELETE SET NULL
);

CREATE TABLE fixed_deposits (
  fd_id INT AUTO_INCREMENT PRIMARY KEY,
  account_id INT NOT NULL,
  principal DECIMAL(15,2) NOT NULL,
  interest_rate DECIMAL(6,3) NOT NULL,
  tenure_months INT NOT NULL,
  start_date DATE,
  maturity_date DATE,
  maturity_amount DECIMAL(15,2),
  status VARCHAR(30),
  CONSTRAINT fk_fd_acc FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE CASCADE
);

CREATE TABLE sip_plans (
  sip_id INT AUTO_INCREMENT PRIMARY KEY,
  account_id INT NOT NULL,
  monthly_amount DECIMAL(15,2) NOT NULL,
  interest_rate DECIMAL(6,3),
  tenure_months INT,
  start_date DATE,
  next_installment_date DATE,
  status VARCHAR(30),
  CONSTRAINT fk_sip_acc FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE CASCADE
);
