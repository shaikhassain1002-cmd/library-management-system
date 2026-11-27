package com.library;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LibraryService {

    public static class Book {
        public final int id;
        public final String title, author, category;
        public final int totalCopies, availableCopies;

        public Book(int id, String title, String author, String category, int totalCopies, int availableCopies) {
            this.id = id;
            this.title = title;
            this.author = author;
            this.category = category;
            this.totalCopies = totalCopies;
            this.availableCopies = availableCopies;
        }
    }

    public static class Member {
        public final int id;
        public final String name, email, phone;

        public Member(int id, String name, String email, String phone) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.phone = phone;
        }
    }

    public static class TransactionRecord {
        public final int txnId, bookId, memberId;
        public final String borrowDate, returnDate, status;

        public TransactionRecord(int txnId, int bookId, int memberId, String borrowDate, String returnDate,
                String status) {
            this.txnId = txnId;
            this.bookId = bookId;
            this.memberId = memberId;
            this.borrowDate = borrowDate;
            this.returnDate = returnDate;
            this.status = status;
        }
    }

    private void validateNonEmpty(String val, String fieldName) throws ValidationException {
        if (val == null || val.trim().isEmpty()) {
            throw new ValidationException(fieldName + " cannot be empty.");
        }
    }

    public void addBook(String title, String author, String category, int totalCopies)
            throws ValidationException, SQLException {
        validateNonEmpty(title, "Title");
        validateNonEmpty(author, "Author");
        validateNonEmpty(category, "Category");
        if (totalCopies <= 0) {
            throw new ValidationException("Total copies must be greater than 0.");
        }

        String sql = "INSERT INTO books (title, author, category, total_copies, available_copies) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title.trim());
            ps.setString(2, author.trim());
            ps.setString(3, category.trim());
            ps.setInt(4, totalCopies);
            ps.setInt(5, totalCopies);
            ps.executeUpdate();
        }
    }

    public void updateBook(int bookId, String title, String author, String category, int totalCopies)
            throws ValidationException, SQLException {
        if (bookId <= 0)
            throw new ValidationException("Select a valid book.");
        validateNonEmpty(title, "Title");
        validateNonEmpty(author, "Author");
        validateNonEmpty(category, "Category");
        if (totalCopies <= 0)
            throw new ValidationException("Total copies must be positive.");

        String sql = "UPDATE books SET title=?, author=?, category=?, total_copies=?, " +
                "available_copies = LEAST(?, GREATEST(0, available_copies + (? - total_copies))) WHERE book_id=?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title.trim());
            ps.setString(2, author.trim());
            ps.setString(3, category.trim());
            ps.setInt(4, totalCopies);
            ps.setInt(5, totalCopies);
            ps.setInt(6, totalCopies);
            ps.setInt(7, bookId);
            if (ps.executeUpdate() == 0) {
                throw new ValidationException("Book ID " + bookId + " not found.");
            }
        }
    }

    public void deleteBook(int bookId) throws ValidationException, SQLException {
        if (bookId <= 0)
            throw new ValidationException("Select a valid book to delete.");
        String sql = "DELETE FROM books WHERE book_id=?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            if (ps.executeUpdate() == 0) {
                throw new ValidationException("Book not found.");
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ValidationException("Cannot delete book with existing borrow transactions.");
        }
    }

    public List<Book> getBooks(String searchText, String categoryFilter, String sortBy) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT book_id, title, author, category, total_copies, available_copies FROM books WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (searchText != null && !searchText.trim().isEmpty()) {
            sql.append("AND (LOWER(title) LIKE ? OR LOWER(author) LIKE ?) ");
            String pattern = "%" + searchText.trim().toLowerCase() + "%";
            params.add(pattern);
            params.add(pattern);
        }

        if (categoryFilter != null && !categoryFilter.equals("All")) {
            sql.append("AND category = ? ");
            params.add(categoryFilter);
        }

        String orderCol;
        switch (sortBy == null ? "" : sortBy) {
            case "Title":
                orderCol = "title ASC";
                break;
            case "Author":
                orderCol = "author ASC";
                break;
            case "Available Copies":
                orderCol = "available_copies DESC";
                break;
            default:
                orderCol = "book_id ASC";
                break;
        }
        sql.append("ORDER BY ").append(orderCol);

        List<Book> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Book(
                            rs.getInt("book_id"),
                            rs.getString("title"),
                            rs.getString("author"),
                            rs.getString("category"),
                            rs.getInt("total_copies"),
                            rs.getInt("available_copies")));
                }
            }
        }
        return list;
    }

    public List<String> getCategories() throws SQLException {
        List<String> cats = new ArrayList<>();
        cats.add("All");
        String sql = "SELECT DISTINCT category FROM books ORDER BY category";
        try (Connection conn = DBConnection.getConnection();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                cats.add(rs.getString("category"));
            }
        }
        return cats;
    }

    public void addMember(String name, String email, String phone) throws ValidationException, SQLException {
        validateNonEmpty(name, "Full Name");
        validateNonEmpty(email, "Email");
        validateNonEmpty(phone, "Phone");
        if (!email.contains("@") || !email.contains(".")) {
            throw new ValidationException("Enter a valid email address.");
        }

        String sql = "INSERT INTO members (full_name, email, phone) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ps.setString(2, email.trim());
            ps.setString(3, phone.trim());
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ValidationException("A member with this email already exists.");
        }
    }

    public void deleteMember(int memberId) throws ValidationException, SQLException {
        if (memberId <= 0)
            throw new ValidationException("Select a valid member.");
        String sql = "DELETE FROM members WHERE member_id=?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            if (ps.executeUpdate() == 0)
                throw new ValidationException("Member not found.");
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ValidationException("Cannot delete member with active/past transactions.");
        }
    }

    public List<Member> getMembers() throws SQLException {
        List<Member> list = new ArrayList<>();
        String sql = "SELECT member_id, full_name, email, phone FROM members ORDER BY member_id";
        try (Connection conn = DBConnection.getConnection();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Member(
                        rs.getInt("member_id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("phone")));
            }
        }
        return list;
    }

    public void borrowBook(int bookId, int memberId) throws ValidationException, SQLException {
        if (bookId <= 0 || memberId <= 0) {
            throw new ValidationException("Valid Book ID and Member ID are required.");
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int available = -1;
                try (PreparedStatement check = conn
                        .prepareStatement("SELECT available_copies FROM books WHERE book_id=?")) {
                    check.setInt(1, bookId);
                    try (ResultSet rs = check.executeQuery()) {
                        if (rs.next())
                            available = rs.getInt("available_copies");
                    }
                }
                if (available == -1)
                    throw new ValidationException("Book ID not found.");
                if (available <= 0)
                    throw new ValidationException("No copies available for borrowing.");

                try (PreparedStatement upd = conn
                        .prepareStatement("UPDATE books SET available_copies = available_copies - 1 WHERE book_id=?")) {
                    upd.setInt(1, bookId);
                    upd.executeUpdate();
                }

                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO transactions (book_id, member_id, borrow_date, status) VALUES (?, ?, ?, 'BORROWED')")) {
                    ins.setInt(1, bookId);
                    ins.setInt(2, memberId);
                    ins.setDate(3, Date.valueOf(LocalDate.now()));
                    ins.executeUpdate();
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void returnBook(int txnId) throws ValidationException, SQLException {
        if (txnId <= 0)
            throw new ValidationException("Enter a valid Transaction ID.");

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int bookId = -1;
                String status = null;
                try (PreparedStatement sel = conn
                        .prepareStatement("SELECT book_id, status FROM transactions WHERE txn_id=?")) {
                    sel.setInt(1, txnId);
                    try (ResultSet rs = sel.executeQuery()) {
                        if (rs.next()) {
                            bookId = rs.getInt("book_id");
                            status = rs.getString("status");
                        }
                    }
                }
                if (bookId == -1)
                    throw new ValidationException("Transaction not found.");
                if ("RETURNED".equals(status))
                    throw new ValidationException("Book is already returned.");

                try (PreparedStatement updTxn = conn.prepareStatement(
                        "UPDATE transactions SET return_date=?, status='RETURNED' WHERE txn_id=?")) {
                    updTxn.setDate(1, Date.valueOf(LocalDate.now()));
                    updTxn.setInt(2, txnId);
                    updTxn.executeUpdate();
                }

                try (PreparedStatement updBook = conn.prepareStatement(
                        "UPDATE books SET available_copies = LEAST(total_copies, available_copies + 1) WHERE book_id=?")) {
                    updBook.setInt(1, bookId);
                    updBook.executeUpdate();
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<TransactionRecord> getTransactions() throws SQLException {
        List<TransactionRecord> list = new ArrayList<>();
        String sql = "SELECT txn_id, book_id, member_id, borrow_date, return_date, status FROM transactions ORDER BY txn_id DESC";
        try (Connection conn = DBConnection.getConnection();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Date ret = rs.getDate("return_date");
                list.add(new TransactionRecord(
                        rs.getInt("txn_id"),
                        rs.getInt("book_id"),
                        rs.getInt("member_id"),
                        rs.getDate("borrow_date").toString(),
                        ret == null ? "-" : ret.toString(),
                        rs.getString("status")));
            }
        }
        return list;
    }
}