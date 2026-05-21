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

### customers

Customer records for sales.

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INT | PRIMARY KEY, AUTO_INCREMENT |
| `name` | VARCHAR(100) | NOT NULL |
| `phone` | VARCHAR(20) | NULL |
| `address` | TEXT | NULL |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |

---

### sales

Sales transactions.

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INT | PRIMARY KEY, AUTO_INCREMENT |
| `customer_id` | INT | FOREIGN KEY → customers(id) |
| `sale_date` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |
| `total_amount` | DOUBLE | DEFAULT 0 |
| `payment_method` | VARCHAR(50) | DEFAULT 'Cash' |
| `sold_by` | INT | FOREIGN KEY → users(id) |
| `status` | VARCHAR(20) | DEFAULT 'Completed' |
| `order_type` | ENUM('pickup', 'deliver') | DEFAULT 'pickup' |
| `notes` | TEXT | NULL |

---

### sales_items

Line items for each sale.

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INT | PRIMARY KEY, AUTO_INCREMENT |
| `sale_id` | INT | FOREIGN KEY → sales(id), ON DELETE CASCADE |
| `product_id` | INT | FOREIGN KEY → products(id) |
| `quantity` | DOUBLE | NOT NULL |
| `unit_price` | DOUBLE | NOT NULL |
| `subtotal` | DOUBLE | DEFAULT 0 |

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
| `unit` | VARCHAR(20) | NULL |
| `notes` | TEXT | NULL |

---

### stock_out

Log of stock reductions (stock out events).

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INT | PRIMARY KEY, AUTO_INCREMENT |
| `stock_id` | INT | NULL |
| `product_name` | VARCHAR(100) | NULL |
| `quantity_out` | DOUBLE | NULL |
| `unit` | VARCHAR(20) | NULL |
| `reason` | VARCHAR(100) | NULL |
| `date_out` | DATE | NULL |
| `notes` | TEXT | NULL |

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

### schedule

Manual events and schedules for the calendar.

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INT | PRIMARY KEY, AUTO_INCREMENT |
| `title` | VARCHAR(100) | NOT NULL |
| `description` | TEXT | NULL |
| `event_date` | DATE | NOT NULL |
| `event_type` | VARCHAR(50) | NULL (e.g., 'Delivery', 'Production', 'Holiday') |
| `created_by` | INT | FOREIGN KEY → users(id) |
| `status` | VARCHAR(20) | DEFAULT 'pending' |

---

### productions

Finished goods ready for sale.

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INT | PRIMARY KEY, AUTO_INCREMENT |
| `name` | VARCHAR(100) | NOT NULL |
| `quantity` | DOUBLE | DEFAULT 0 |
| `price` | DOUBLE | DEFAULT 0 |
| `unit` | VARCHAR(20) | NULL |
| `status` | VARCHAR(20) | DEFAULT 'No Stock' |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |

---

### production_ingredients

Mapping of raw materials used in each production run.

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INT | PRIMARY KEY, AUTO_INCREMENT |
| `production_id` | INT | FOREIGN KEY → productions(id) |
| `stock_id` | INT | FOREIGN KEY → stocks(id) |
| `quantity_used` | DOUBLE | NOT NULL |

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

CREATE TABLE customers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sales (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT,
    sale_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount DOUBLE DEFAULT 0,
    payment_method VARCHAR(50) DEFAULT 'Cash',
    sold_by INT,
    status VARCHAR(20) DEFAULT 'Completed',
    order_type ENUM('pickup', 'deliver') DEFAULT 'pickup',
    notes TEXT,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (sold_by) REFERENCES users(id)
);

CREATE TABLE sales_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sale_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity DOUBLE NOT NULL,
    unit_price DOUBLE NOT NULL,
    subtotal DOUBLE DEFAULT 0,
    FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE stock_in_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100),
    quantity DOUBLE,
    supplier_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    unit VARCHAR(20),
    notes TEXT
);

CREATE TABLE stock_out (
    id INT AUTO_INCREMENT PRIMARY KEY,
    stock_id INT,
    product_name VARCHAR(100),
    quantity_out DOUBLE,
    unit VARCHAR(20),
    reason VARCHAR(100),
    date_out DATE,
    notes TEXT
);

CREATE TABLE sales_orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    supplier_name VARCHAR(100),
    order_date DATE,
    status VARCHAR(20) DEFAULT 'Pending',
    total_amount DOUBLE DEFAULT 0,
    notes TEXT
);

CREATE TABLE schedule (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    event_date DATE NOT NULL,
    event_type VARCHAR(50),
    created_by INT,
    status VARCHAR(20) DEFAULT 'pending',
    FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE productions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    quantity DOUBLE DEFAULT 0,
    price DOUBLE DEFAULT 0,
    unit VARCHAR(20),
    status VARCHAR(20) DEFAULT 'No Stock',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE production_ingredients (
    id INT AUTO_INCREMENT PRIMARY KEY,
    production_id INT NOT NULL,
    stock_id INT NOT NULL,
    quantity_used DOUBLE NOT NULL,
    FOREIGN KEY (production_id) REFERENCES productions(id) ON DELETE CASCADE,
    FOREIGN KEY (stock_id) REFERENCES stocks(id)
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

---

## ALTER Scripts (for existing database)

Run these to update your existing tables:

```sql
-- 1. Create customers table (new)
CREATE TABLE IF NOT EXISTS customers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Update sales table
ALTER TABLE sales
    ADD COLUMN customer_id INT,
    ADD COLUMN payment_method VARCHAR(50) DEFAULT 'Cash',
    ADD COLUMN sold_by INT,
    ADD COLUMN status VARCHAR(20) DEFAULT 'Completed',
    ADD COLUMN order_type ENUM('pickup', 'deliver') DEFAULT 'pickup',
    ADD COLUMN notes TEXT;

ALTER TABLE sales
    ADD FOREIGN KEY (customer_id) REFERENCES customers(id),
    ADD FOREIGN KEY (sold_by) REFERENCES users(id);

-- 3. Update sales_items table
ALTER TABLE sales_items
    ADD COLUMN unit_price DOUBLE NOT NULL AFTER quantity,
    ADD COLUMN subtotal DOUBLE DEFAULT 0 AFTER unit_price;
```
