# Database Schema - Stock Savvy Longganisa

## Connection

| Property | Value           |
|----------|-----------------|
| Host | `localhost`     |
| Port | `3306`          |
| Database | `stocksavvy`    |
| User | `secret`        |
| Password | (set in `.env`) |

## Tables

### users

Stores user accounts for authentication.

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INT | PRIMARY KEY, AUTO_INCREMENT |
| `username` | VARCHAR(50) | UNIQUE, NOT NULL |
| `password` | VARCHAR(255) | NOT NULL |
| `role` | VARCHAR(20) | DEFAULT 'Staff' |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |

**Login Query:**
```sql
SELECT * FROM users WHERE username=? AND password=? AND role=?
```

**Test Data:**
```sql
INSERT INTO users (username, password, role) VALUES ('admin', 'admin123', 'Admin');
INSERT INTO users (username, password, role) VALUES ('staff', 'staff123', 'Staff');
```

---

### stocks

Inventory items with stock tracking.

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INT | PRIMARY KEY, AUTO_INCREMENT |
| `product_name` | VARCHAR(100) | NOT NULL |
| `category` | VARCHAR(50) | NULL |
| `quantity` | DOUBLE | DEFAULT 0 |
| `unit` | VARCHAR(20) | NULL |
| `cost_per_unit` | DOUBLE | DEFAULT 0 |
| `supplier` | VARCHAR(100) | NULL |
| `date_received` | DATE | NULL |
| `notes` | TEXT | NULL |
| `expiry_date` | DATE | NULL |
| `low_stock_threshold` | INT | DEFAULT 5 |

---

### products

Product catalog for sales.

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INT | PRIMARY KEY, AUTO_INCREMENT |
| `name` | VARCHAR(100) | NOT NULL |
| `category` | VARCHAR(50) | NULL |
| `price` | DOUBLE | DEFAULT 0 |

---

### sales

Sales transactions.

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INT | PRIMARY KEY, AUTO_INCREMENT |
| `sale_date` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |
| `total_amount` | DOUBLE | DEFAULT 0 |
| `customer_name` | VARCHAR(100) | NULL |
| `created_by` | VARCHAR(50) | NULL |

---

### sales_items

Line items for each sale.

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INT | PRIMARY KEY, AUTO_INCREMENT |
| `sale_id` | INT | FOREIGN KEY → sales(id) |
| `product_id` | INT | FOREIGN KEY → products(id) |
| `quantity` | INT | NOT NULL |
| `price_per_unit` | DOUBLE | NOT NULL |

---

### stock_in_log

Log of stock additions.

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INT | PRIMARY KEY, AUTO_INCREMENT |
| `product_name` | VARCHAR(100) | NULL |
| `quantity` | DOUBLE | NULL |
| `supplier_name` | VARCHAR(100) | NULL |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |

---

### sales_orders

Purchase orders for restocking.

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INT | PRIMARY KEY, AUTO_INCREMENT |
| `supplier_name` | VARCHAR(100) | NULL |
| `order_date` | DATE | NULL |
| `status` | VARCHAR(20) | DEFAULT 'Pending' |
| `total_amount` | DOUBLE | DEFAULT 0 |
| `notes` | TEXT | NULL |

---

## Quick Setup

Run all tables at once:

```sql
CREATE DATABASE stocksavvy;
USE stocksavvy;

CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'Staff',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE stocks (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    quantity DOUBLE DEFAULT 0,
    unit VARCHAR(20),
    cost_per_unit DOUBLE DEFAULT 0,
    supplier VARCHAR(100),
    date_received DATE,
    notes TEXT,
    expiry_date DATE,
    low_stock_threshold INT DEFAULT 5
);

CREATE TABLE products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    price DOUBLE DEFAULT 0
);

CREATE TABLE sales (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sale_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount DOUBLE DEFAULT 0,
    customer_name VARCHAR(100),
    created_by VARCHAR(50)
);

CREATE TABLE sales_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sale_id INT,
    product_id INT,
    quantity INT,
    price_per_unit DOUBLE,
    FOREIGN KEY (sale_id) REFERENCES sales(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE stock_in_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100),
    quantity DOUBLE,
    supplier_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sales_orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    supplier_name VARCHAR(100),
    order_date DATE,
    status VARCHAR(20) DEFAULT 'Pending',
    total_amount DOUBLE DEFAULT 0,
    notes TEXT
);

INSERT INTO users (username, password, role) VALUES ('admin', 'admin123', 'Admin');
INSERT INTO users (username, password, role) VALUES ('staff', 'staff123', 'Staff');
```

## Environment Variables (.env)

```env
DB_HOST=localhost
DB_PORT=3306
DB_NAME=stocksavvy
DB_USER=root
DB_PASSWORD=your_password_here
```