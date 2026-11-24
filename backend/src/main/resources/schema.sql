-- 高校教材订购与库存全流程管理系统 - 数据库表结构脚本 (兼容 H2 MySQL 模式与 MySQL 8.0)

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(50),
    role VARCHAR(20) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS book (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    isbn VARCHAR(30) UNIQUE,
    name VARCHAR(120) NOT NULL,
    author VARCHAR(80),
    publisher VARCHAR(100),
    price DECIMAL(10, 2) DEFAULT 0.00,
    category VARCHAR(50),
    description TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id BIGINT NOT NULL UNIQUE,
    quantity INT DEFAULT 0,
    warning_quantity INT DEFAULT 20,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_book FOREIGN KEY (book_id) REFERENCES book(id)
);

CREATE TABLE IF NOT EXISTS book_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    total_price DECIMAL(10, 2) DEFAULT 0.00,
    status TINYINT DEFAULT 0,
    reviewer_id BIGINT,
    review_time DATETIME,
    review_remark VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_order_book FOREIGN KEY (book_id) REFERENCES book(id)
);

CREATE TABLE IF NOT EXISTS shortage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id BIGINT NOT NULL,
    required_quantity INT NOT NULL,
    current_quantity INT DEFAULT 0,
    status TINYINT DEFAULT 0,
    order_id BIGINT,
    remark VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    CONSTRAINT fk_shortage_book FOREIGN KEY (book_id) REFERENCES book(id)
);

CREATE TABLE IF NOT EXISTS pending_purchase (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shortage_id BIGINT,
    book_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    status TINYINT DEFAULT 0,
    purchaser_id BIGINT,
    expected_date DATE,
    remark VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    CONSTRAINT fk_purchase_shortage FOREIGN KEY (shortage_id) REFERENCES shortage(id),
    CONSTRAINT fk_purchase_book FOREIGN KEY (book_id) REFERENCES book(id)
);

CREATE TABLE IF NOT EXISTS stock_in (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    purchase_id BIGINT,
    operator_id BIGINT NOT NULL,
    batch_no VARCHAR(32),
    remark VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stock_in_book FOREIGN KEY (book_id) REFERENCES book(id),
    CONSTRAINT fk_stock_in_purchase FOREIGN KEY (purchase_id) REFERENCES pending_purchase(id),
    CONSTRAINT fk_stock_in_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS stock_out (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    order_id BIGINT,
    operator_id BIGINT NOT NULL,
    remark VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stock_out_book FOREIGN KEY (book_id) REFERENCES book(id),
    CONSTRAINT fk_stock_out_order FOREIGN KEY (order_id) REFERENCES book_order(id),
    CONSTRAINT fk_stock_out_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id)
);
