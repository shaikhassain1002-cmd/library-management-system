# Library Management System (Java + MySQL)

## Overview
A desktop Library Management System built with Java (Swing), JDBC, and MySQL to manage books, members, and book borrowing/returning workflows.

## Architecture
- **UI Layer (`LibraryApp.java`):** Java Swing tabbed GUI (`Books`, `Members`, `Borrow / Return`) with search, category filtering, sorting, and form actions.
- **Service & Validation Layer (`LibraryService.java`, `ValidationException.java`):** Handles input validation, custom validation exceptions, CRUD operations, dynamic SQL queries, and transactional borrow/return updates.
- **Database Layer (`DBConnection.java`, `schema.sql`):** Connects via JDBC (`PreparedStatement` and manual transaction rollback/commit) to MySQL (`library_db`).

## Database Schema
- `books(book_id, title, author, category, total_copies, available_copies)`
- `members(member_id, full_name, email, phone)`
- `transactions(txn_id, book_id, member_id, borrow_date, return_date, status)`

## Setup & Usage
1. Run `schema.sql` in MySQL Workbench to create `library_db`, tables, and sample data.
2. Update MySQL credentials in `src/com/library/DBConnection.java`.
3. Ensure `mysql-connector-j-*.jar` is present in `lib/`.
4. Run `LibraryApp.java`.