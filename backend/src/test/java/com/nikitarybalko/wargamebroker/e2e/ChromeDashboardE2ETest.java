package com.nikitarybalko.wargamebroker.e2e;

import org.junit.jupiter.api.DisplayName;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.chrome.ChromeOptions;

@DisplayName("E2E tests: Commander dashboard in Chrome")
class ChromeDashboardE2ETest extends AbstractDashboardE2ETest {

    @Override
    protected Capabilities capabilities() {
        return new ChromeOptions();
    }
}
