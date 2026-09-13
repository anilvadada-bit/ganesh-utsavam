package com.ganesh.ganesh_utsavam.controller;

import com.ganesh.ganesh_utsavam.entity.Expense;
import com.ganesh.ganesh_utsavam.repository.ExpenseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/expenses")
public class ExpenseController {

    private final ExpenseRepository expenseRepository;

    public ExpenseController(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    // Get all expenses
    @GetMapping
    public List<Expense> getAllExpenses() {

        return expenseRepository
                .findAllByOrderByExpenseDateDesc();
    }

    // Add new expense
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Expense addExpense(
            @RequestBody Expense expense) {

        if (expense.getCategory() == null ||
                expense.getCategory().isBlank()) {

            throw new IllegalArgumentException(
                    "Expense category is required"
            );
        }

        if (expense.getAmount() == null ||
                expense.getAmount()
                        .compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Expense amount must be greater than zero"
            );
        }

        expense.setCategory(
                expense.getCategory().trim()
        );

        if (expense.getDescription() != null) {

            expense.setDescription(
                    expense.getDescription().trim()
            );
        }

        if (expense.getExpenseDate() == null) {

            expense.setExpenseDate(
                    LocalDateTime.now()
            );
        }

        return expenseRepository.save(expense);
    }

    // Delete expense
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteExpense(
            @PathVariable Long id) {

        if (!expenseRepository.existsById(id)) {

            throw new IllegalArgumentException(
                    "Expense not found with ID: " + id
            );
        }

        expenseRepository.deleteById(id);

        return Map.of(
                "success", true,
                "message", "Expense deleted successfully",
                "expenseId", id
        );
    }
}