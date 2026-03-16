CREATE TABLE IF NOT EXISTS sys_user (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    mobile VARCHAR(255),
    email VARCHAR(255),
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    avatar VARCHAR(255),
    enable_flag BOOLEAN DEFAULT TRUE,
    login_time TIMESTAMP,
    create_by VARCHAR(255),
    create_time TIMESTAMP,
    update_by VARCHAR(255),
    update_time TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_username ON sys_user(username);
CREATE UNIQUE INDEX IF NOT EXISTS uk_mobile ON sys_user(mobile);
CREATE UNIQUE INDEX IF NOT EXISTS uk_email ON sys_user(email);

CREATE TABLE IF NOT EXISTS proxy_client (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    token VARCHAR(255) NOT NULL,
    status VARCHAR(50),
    enable_flag BOOLEAN DEFAULT TRUE,
    create_by VARCHAR(255),
    create_time TIMESTAMP,
    update_by VARCHAR(255),
    update_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS proxy_client_rule (
    id INT AUTO_INCREMENT PRIMARY KEY,
    proxy_client_id INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    server_port INT NOT NULL,
    client_address VARCHAR(255) NOT NULL,
    limit_conn INT,
    limit_rate INT,
    enable_flag BOOLEAN,
    create_by VARCHAR(255),
    create_time TIMESTAMP,
    update_by VARCHAR(255),
    update_time TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_proxy_client_rule_client ON proxy_client_rule(proxy_client_id);

CREATE TABLE IF NOT EXISTS sys_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    log_type VARCHAR(50),
    log_content VARCHAR(2000),
    create_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ts_report (
    id INT AUTO_INCREMENT PRIMARY KEY,
    proxy_client_id INT NOT NULL,
    proxy_client_rule_id INT NOT NULL,
    upload_bytes BIGINT DEFAULT 0,
    download_bytes BIGINT DEFAULT 0,
    create_time TIMESTAMP,
    update_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ts_report_client ON ts_report(proxy_client_id);
CREATE INDEX IF NOT EXISTS idx_ts_report_rule ON ts_report(proxy_client_rule_id);

CREATE TABLE IF NOT EXISTS ts_day_report (
    id INT AUTO_INCREMENT PRIMARY KEY,
    proxy_client_id INT NOT NULL,
    proxy_client_rule_id INT NOT NULL,
    date DATE NOT NULL,
    upward_traffic_bytes BIGINT,
    downward_traffic_bytes BIGINT,
    create_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ts_day_report_client ON ts_day_report(proxy_client_id);
CREATE INDEX IF NOT EXISTS idx_ts_day_report_rule ON ts_day_report(proxy_client_rule_id);

CREATE TABLE IF NOT EXISTS ts_hour_report (
    id INT AUTO_INCREMENT PRIMARY KEY,
    proxy_client_id INT NOT NULL,
    proxy_client_rule_id INT NOT NULL,
    date TIMESTAMP NOT NULL,
    upward_traffic_bytes BIGINT,
    downward_traffic_bytes BIGINT,
    create_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ts_hour_report_client ON ts_hour_report(proxy_client_id);
CREATE INDEX IF NOT EXISTS idx_ts_hour_report_rule ON ts_hour_report(proxy_client_rule_id);
