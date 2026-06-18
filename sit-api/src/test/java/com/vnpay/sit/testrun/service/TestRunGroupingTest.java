package com.vnpay.sit.testrun.service;

import com.vnpay.sit.model.CallbackType;
import com.vnpay.sit.model.TestCaseType;
import com.vnpay.sit.testrun.entity.TestRun;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TestRunGroupingTest {

    @Test
    void effectiveByTestCase_shouldPreferLatestPassedOverNewerFailedReRun() {
        TestRun passedSuccess = run(10L, TestCaseType.SUCCESS, true, "00");
        TestRun failedReRun = run(20L, TestCaseType.SUCCESS, false, "02");

        Map<TestCaseType, TestRun> effective = TestRunGrouping.effectiveByTestCase(
                List.of(failedReRun, passedSuccess),
                run -> true
        );

        assertThat(effective.get(TestCaseType.SUCCESS)).isSameAs(passedSuccess);
    }

    @Test
    void effectiveByTestCase_withoutPassedRun_shouldUseLatestAny() {
        TestRun olderFail = run(5L, TestCaseType.SUCCESS, false, "01");
        TestRun newerFail = run(15L, TestCaseType.SUCCESS, false, "02");

        Map<TestCaseType, TestRun> effective = TestRunGrouping.effectiveByTestCase(
                List.of(newerFail, olderFail),
                run -> true
        );

        assertThat(effective.get(TestCaseType.SUCCESS)).isSameAs(newerFail);
    }

    @Test
    void effectiveByTestCase_shouldUseNewerPassedAfterFix() {
        TestRun oldFail = run(8L, TestCaseType.SUCCESS, false, "01");
        TestRun newPass = run(18L, TestCaseType.SUCCESS, true, "00");

        Map<TestCaseType, TestRun> effective = TestRunGrouping.effectiveByTestCase(
                List.of(newPass, oldFail),
                run -> true
        );

        assertThat(effective.get(TestCaseType.SUCCESS)).isSameAs(newPass);
    }

    private static TestRun run(long id, TestCaseType testCase, boolean passed, String actualRsp) {
        TestRun run = new TestRun();
        run.setId(id);
        run.setTestCase(testCase);
        run.setCallbackType(CallbackType.IPN);
        run.setPassed(passed);
        run.setActualRspCode(actualRsp);
        return run;
    }
}
