package com.hrms.util;

import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates employee codes in the format EMP-{year}-{sequence}.
 * Backed by an in-memory sequence seeded from the current count;
 * uniqueness is still enforced at the database level via a unique constraint.
 */
@Component
public class EmployeeCodeGenerator {

    private final AtomicLong sequence = new AtomicLong(0);

    public String generate(long currentEmployeeCount) {
        long seq = sequence.updateAndGet(prev -> Math.max(prev, currentEmployeeCount) + 1);
        return String.format("EMP-%d-%05d", Year.now().getValue(), seq);
    }
}
