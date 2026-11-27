package com.library;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class LibraryApp extends JFrame {
    private final LibraryService service = new LibraryService();

    private DefaultTableModel bookModel, memberModel, txnModel;
    private JTextField txtSearch, txtTitle, txtAuthor, txtCategory, txtCopies;
    private JComboBox<String> cmbCategoryFilter, cmbSort;

    private JTextField txtMemberName, txtEmail, txtPhone;
    private JTextField txtBorrowBookId, txtBorrowMemberId, txtReturnTxnId;

    public LibraryApp() {
        setTitle("Library Management System (Java + MySQL)");
        setSize(950, 620);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Books", createBooksPanel());
        tabs.addTab("Members", createMembersPanel());
        tabs.addTab("Borrow / Return", createTransactionsPanel());

        add(tabs);
        refreshAll();
    }

    private JPanel createBooksPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        txtSearch = new JTextField(16);
        cmbCategoryFilter = new JComboBox<>();
        cmbSort = new JComboBox<>(new String[] { "Default", "Title", "Author", "Available Copies" });
        JButton btnApplyFilter = new JButton("Search / Apply");
        JButton btnReset = new JButton("Reset");

        topBar.add(new JLabel("Search:"));
        topBar.add(txtSearch);
        topBar.add(new JLabel("Category:"));
        topBar.add(cmbCategoryFilter);
        topBar.add(new JLabel("Sort:"));
        topBar.add(cmbSort);
        topBar.add(btnApplyFilter);
        topBar.add(btnReset);

        bookModel = new DefaultTableModel(new String[] { "ID", "Title", "Author", "Category", "Total", "Available" },
                0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable bookTable = new JTable(bookModel);

        JPanel form = new JPanel(new GridLayout(2, 1, 5, 5));
        JPanel fields = new JPanel(new FlowLayout(FlowLayout.LEFT));
        txtTitle = new JTextField(14);
        txtAuthor = new JTextField(12);
        txtCategory = new JTextField(10);
        txtCopies = new JTextField(5);

        fields.add(new JLabel("Title:"));
        fields.add(txtTitle);
        fields.add(new JLabel("Author:"));
        fields.add(txtAuthor);
        fields.add(new JLabel("Category:"));
        fields.add(txtCategory);
        fields.add(new JLabel("Copies:"));
        fields.add(txtCopies);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAdd = new JButton("Add Book");
        JButton btnUpdate = new JButton("Update Selected");
        JButton btnDelete = new JButton("Delete Selected");
        actions.add(btnAdd);
        actions.add(btnUpdate);
        actions.add(btnDelete);

        form.add(fields);
        form.add(actions);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(bookTable), BorderLayout.CENTER);
        panel.add(form, BorderLayout.SOUTH);

        bookTable.getSelectionModel().addListSelectionListener(e -> {
            int row = bookTable.getSelectedRow();
            if (row >= 0) {
                txtTitle.setText(bookModel.getValueAt(row, 1).toString());
                txtAuthor.setText(bookModel.getValueAt(row, 2).toString());
                txtCategory.setText(bookModel.getValueAt(row, 3).toString());
                txtCopies.setText(bookModel.getValueAt(row, 4).toString());
            }
        });

        btnApplyFilter.addActionListener(e -> loadBooks());
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            cmbCategoryFilter.setSelectedItem("All");
            cmbSort.setSelectedItem("Default");
            loadBooks();
        });

        btnAdd.addActionListener(e -> {
            try {
                int copies = parsePositiveInt(txtCopies.getText(), "Copies");
                service.addBook(txtTitle.getText(), txtAuthor.getText(), txtCategory.getText(), copies);
                clearBookInputs();
                refreshCategoriesAndBooks();
                showInfo("Book added!");
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        btnUpdate.addActionListener(e -> {
            int row = bookTable.getSelectedRow();
            if (row < 0) {
                showError("Select a book row first.");
                return;
            }
            try {
                int id = (int) bookModel.getValueAt(row, 0);
                int copies = parsePositiveInt(txtCopies.getText(), "Copies");
                service.updateBook(id, txtTitle.getText(), txtAuthor.getText(), txtCategory.getText(), copies);
                refreshCategoriesAndBooks();
                showInfo("Book updated!");
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        btnDelete.addActionListener(e -> {
            int row = bookTable.getSelectedRow();
            if (row < 0) {
                showError("Select a book row first.");
                return;
            }
            try {
                int id = (int) bookModel.getValueAt(row, 0);
                service.deleteBook(id);
                clearBookInputs();
                refreshCategoriesAndBooks();
                showInfo("Book deleted!");
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        return panel;
    }

    private JPanel createMembersPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        memberModel = new DefaultTableModel(new String[] { "Member ID", "Name", "Email", "Phone" }, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable memberTable = new JTable(memberModel);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        txtMemberName = new JTextField(14);
        txtEmail = new JTextField(16);
        txtPhone = new JTextField(12);
        JButton btnAddMember = new JButton("Add Member");
        JButton btnDeleteMember = new JButton("Delete Selected");

        bottom.add(new JLabel("Name:"));
        bottom.add(txtMemberName);
        bottom.add(new JLabel("Email:"));
        bottom.add(txtEmail);
        bottom.add(new JLabel("Phone:"));
        bottom.add(txtPhone);
        bottom.add(btnAddMember);
        bottom.add(btnDeleteMember);

        panel.add(new JScrollPane(memberTable), BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);

        btnAddMember.addActionListener(e -> {
            try {
                service.addMember(txtMemberName.getText(), txtEmail.getText(), txtPhone.getText());
                txtMemberName.setText("");
                txtEmail.setText("");
                txtPhone.setText("");
                loadMembers();
                showInfo("Member added!");
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        btnDeleteMember.addActionListener(e -> {
            int row = memberTable.getSelectedRow();
            if (row < 0) {
                showError("Select a member first.");
                return;
            }
            try {
                int id = (int) memberModel.getValueAt(row, 0);
                service.deleteMember(id);
                loadMembers();
                showInfo("Member deleted!");
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        return panel;
    }

    private JPanel createTransactionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        txtBorrowBookId = new JTextField(6);
        txtBorrowMemberId = new JTextField(6);
        JButton btnBorrow = new JButton("Borrow Book");

        txtReturnTxnId = new JTextField(6);
        JButton btnReturn = new JButton("Return Book");

        top.add(new JLabel("Book ID:"));
        top.add(txtBorrowBookId);
        top.add(new JLabel("Member ID:"));
        top.add(txtBorrowMemberId);
        top.add(btnBorrow);
        top.add(Box.createHorizontalStrut(20));
        top.add(new JLabel("Txn ID:"));
        top.add(txtReturnTxnId);
        top.add(btnReturn);

        txnModel = new DefaultTableModel(
                new String[] { "Txn ID", "Book ID", "Member ID", "Borrow Date", "Return Date", "Status" }, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable txnTable = new JTable(txnModel);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(txnTable), BorderLayout.CENTER);

        txnTable.getSelectionModel().addListSelectionListener(e -> {
            int row = txnTable.getSelectedRow();
            if (row >= 0) {
                txtReturnTxnId.setText(txnModel.getValueAt(row, 0).toString());
            }
        });

        btnBorrow.addActionListener(e -> {
            try {
                int bookId = parsePositiveInt(txtBorrowBookId.getText(), "Book ID");
                int memberId = parsePositiveInt(txtBorrowMemberId.getText(), "Member ID");
                service.borrowBook(bookId, memberId);
                txtBorrowBookId.setText("");
                txtBorrowMemberId.setText("");
                refreshAll();
                showInfo("Book borrowed!");
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        btnReturn.addActionListener(e -> {
            try {
                int txnId = parsePositiveInt(txtReturnTxnId.getText(), "Transaction ID");
                service.returnBook(txnId);
                txtReturnTxnId.setText("");
                refreshAll();
                showInfo("Book returned!");
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        return panel;
    }

    private int parsePositiveInt(String text, String label) throws ValidationException {
        try {
            int val = Integer.parseInt(text.trim());
            if (val <= 0)
                throw new ValidationException(label + " must be positive.");
            return val;
        } catch (NumberFormatException e) {
            throw new ValidationException(label + " must be a valid number.");
        }
    }

    private void clearBookInputs() {
        txtTitle.setText("");
        txtAuthor.setText("");
        txtCategory.setText("");
        txtCopies.setText("");
    }

    private void refreshAll() {
        refreshCategoriesAndBooks();
        loadMembers();
        loadTransactions();
    }

    private void refreshCategoriesAndBooks() {
        try {
            Object selected = cmbCategoryFilter.getSelectedItem();
            List<String> categories = service.getCategories();
            cmbCategoryFilter.removeAllItems();
            for (String c : categories)
                cmbCategoryFilter.addItem(c);
            if (selected != null && categories.contains(selected.toString())) {
                cmbCategoryFilter.setSelectedItem(selected);
            }
            loadBooks();
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    private void loadBooks() {
        try {
            String cat = cmbCategoryFilter.getSelectedItem() == null ? "All"
                    : cmbCategoryFilter.getSelectedItem().toString();
            String sort = cmbSort.getSelectedItem() == null ? "Default" : cmbSort.getSelectedItem().toString();
            List<LibraryService.Book> books = service.getBooks(txtSearch.getText(), cat, sort);
            bookModel.setRowCount(0);
            for (LibraryService.Book b : books) {
                bookModel
                        .addRow(new Object[] { b.id, b.title, b.author, b.category, b.totalCopies, b.availableCopies });
            }
        } catch (SQLException e) {
            showError("Failed to load books: " + e.getMessage());
        }
    }

    private void loadMembers() {
        try {
            List<LibraryService.Member> members = service.getMembers();
            memberModel.setRowCount(0);
            for (LibraryService.Member m : members) {
                memberModel.addRow(new Object[] { m.id, m.name, m.email, m.phone });
            }
        } catch (SQLException e) {
            showError("Failed to load members: " + e.getMessage());
        }
    }

    private void loadTransactions() {
        try {
            List<LibraryService.TransactionRecord> txns = service.getTransactions();
            txnModel.setRowCount(0);
            for (LibraryService.TransactionRecord t : txns) {
                txnModel.addRow(new Object[] { t.txnId, t.bookId, t.memberId, t.borrowDate, t.returnDate, t.status });
            }
        } catch (SQLException e) {
            showError("Failed to load transactions: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LibraryApp().setVisible(true));
    }
}