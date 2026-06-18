package com.vnpay.sit.testrun.service;

import com.vnpay.sit.model.TestCaseType;
import com.vnpay.sit.testrun.entity.TestRun;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Chọn run đại diện theo test case. Ưu tiên lần chạy PASS mới nhất để tránh
 * ghi đè kết quả nghiệm thu khi chạy lại Case 5/6 sau khi đơn đã xử lý (RspCode 02).
 */
public final class TestRunGrouping {

    private TestRunGrouping() {
    }

    public static Map<TestCaseType, TestRun> effectiveByTestCase(
            List<TestRun> runsNewestFirst,
            Predicate<TestRun> filter
    ) {
        Map<TestCaseType, TestRun> latestAny = new EnumMap<>(TestCaseType.class);
        Map<TestCaseType, TestRun> latestPassed = new EnumMap<>(TestCaseType.class);

        for (TestRun run : runsNewestFirst) {
            if (run.getTestCase() == null || !filter.test(run)) {
                continue;
            }
            TestCaseType testCase = run.getTestCase();
            latestAny.putIfAbsent(testCase, run);
            if (run.isPassed()) {
                latestPassed.putIfAbsent(testCase, run);
            }
        }

        Map<TestCaseType, TestRun> effective = new EnumMap<>(TestCaseType.class);
        for (Map.Entry<TestCaseType, TestRun> entry : latestAny.entrySet()) {
            TestRun passed = latestPassed.get(entry.getKey());
            effective.put(entry.getKey(), passed != null ? passed : entry.getValue());
        }
        return effective;
    }

    public static List<TestRun> effectiveRunsList(
            List<TestRun> runsNewestFirst,
            Predicate<TestRun> filter
    ) {
        return new ArrayList<>(effectiveByTestCase(runsNewestFirst, filter).values());
    }
}
