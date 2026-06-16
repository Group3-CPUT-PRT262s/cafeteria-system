CREATE TABLE APP_USER (
    user_id INT PRIMARY KEY, 
    username VARCHAR(20) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR (15),
    last_name VARCHAR(12),
    email VARCHAR(70) UNIQUE,
    role VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE TIME_SLOT(
    time_slot_id INT PRIMARY KEY,
    slot_time VARCHAR(30),
    max_orders INT,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE CATEGORY(
    category_id INT PRIMARY KEY,
    category_name VARCHAR(60) NOT NULL
);


CREATE TABLE MENU_ITEM (
    menu_item_id INT PRIMARY KEY,
    category_id INT REFERENCES CATEGORY(category_id),
    item_name VARCHAR(20) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2), 
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES CATEGORY(category_id)
);

CREATE TABLE CUSTOMER_ORDER(
    order_id INT PRIMARY KEY,
    user_id INT,
    time_slot_id INT,
    total_amount DECIMAL(10,2),
    order_status VARCHAR(12),
    ordered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE ORDER_ITEM(
    order_item_id INT PRIMARY KEY,
    order_id INT,
    menu_item_id INT,
    quantity INT, 
    unit_price DECIMAL(10, 2),
    subtotal DECIMAL(10, 2)
    );

CREATE TABLE PAYMENT(
    payment_id INT PRIMARY KEY,
    order_id INT UNIQUE,
    amound_paid DECIMAL(10,2),
    payment_method VARCHAR(30),
    payment_status VARCHAR (20),
    transaction_reference VARCHAR(100),
    paid_at TIMESTAMP
    );

CREATE TABLE INVENTORY (
    inventory_id INT PRIMARY KEY,
    menu_item_id INT UNIQUE,
    quantity_in_stock INT,
    unit_measure VARCHAR(20),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );