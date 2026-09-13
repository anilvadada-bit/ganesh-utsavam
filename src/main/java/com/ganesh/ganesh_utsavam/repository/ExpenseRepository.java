package com.ganesh.ganesh_utsavam.repository;

import com.ganesh.ganesh_utsavam.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findAllByOrderByExpenseDateDesc();

    @Query("""
            SELECT COALESCE(SUM(e.amount), 0)
            FROM Expense e
            """)
    BigDecimal getTotalExpenseAmount();
}