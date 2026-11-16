package com.nikitarybalko.wargamebroker.e2e;

import org.junit.jupiter.api.DisplayName;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.firefox.FirefoxOptions;

@DisplayName("E2E tests: Commander dashboard in Firefox")
class FirefoxDashboardE2ETest extends AbstractDashboardE2ETest {

    @Override
    protected Capabilities capabilities() {
        return new FirefoxOptions();
    }
}
