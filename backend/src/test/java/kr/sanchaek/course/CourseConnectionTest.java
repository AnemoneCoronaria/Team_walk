package kr.sanchaek.course;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SpringBootTest(properties = {
        "spring.datasource.hikari.maximum-pool-size=1",
        "spring.datasource.hikari.connection-timeout=1000"
})
class CourseConnectionTest {
    @Autowired CourseRepository courses;

    @Test void nestedReadsWorkWithoutCallerTransactionWithOnlyOneConnection() {
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        var page = courses.list(null, null, null, null, null, null, "distance", 1, 20);
        assertFalse(page.items().isEmpty());
        assertFalse(page.items().get(0).facilities().isEmpty());
        var detail = courses.detail(4, null);
        assertFalse(detail.facilities().isEmpty());
        assertFalse(detail.landmarks().isEmpty());
    }
}
