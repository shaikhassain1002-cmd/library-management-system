CREATE DATABASE IF NOT EXISTS library_db;
USE library_db;

CREATE TABLE books (
    book_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(150) NOT NULL,
    category VARCHAR(80) NOT NULL,
    total_copies INT NOT NULL DEFAULT 1 CHECK (total_copies >= 0),
    available_copies INT NOT NULL DEFAULT 1 CHECK (available_copies >= 0)
);

CREATE TABLE members (
    member_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL
);

CREATE TABLE transactions (
    txn_id INT AUTO_INCREMENT PRIMARY KEY,
    book_id INT NOT NULL,
    member_id INT NOT NULL,
    borrow_date DATE NOT NULL,
    return_date DATE NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'BORROWED',
    CONSTRAINT fk_txn_book FOREIGN KEY (book_id) REFERENCES books(book_id),
    CONSTRAINT fk_txn_member FOREIGN KEY (member_id) REFERENCES members(member_id)
);

INSERT INTO books (title, author, category, total_copies, available_copies) VALUES
('Clean Code', 'Robert C. Martin', 'Programming', 4, 4),
('Effective Java', 'Joshua Bloch', 'Programming', 3, 3),
('Atomic Habits', 'James Clear', 'Self-Help', 5, 5),
('The Pragmatic Programmer', 'Andrew Hunt', 'Programming', 2, 2);

INSERT INTO members (full_name, email, phone) VALUES
('Aarav Sharma', 'aarav@example.com', '9876543210'),
('Meera Nair', 'meera@example.com', '9876543211');