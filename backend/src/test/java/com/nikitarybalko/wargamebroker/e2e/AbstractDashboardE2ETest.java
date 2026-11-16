package com.nikitarybalko.wargamebroker.e2e;

import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.domain.model.SimulationStatus;
import com.nikitarybalko.wargamebroker.domain.model.UnitStatus;
import com.nikitarybalko.wargamebroker.domain.model.UnitType;
import com.nikitarybalko.wargamebroker.web.dto.UnitDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BrowserWebDriverContainer;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.stream.LongStream;

import static com.nikitarybalko.wargamebroker.support.TestCases.TC_FR10_01;
import static com.nikitarybalko.wargamebroker.support.TestCases.TC_FR10_02;
import static com.nikitarybalko.wargamebroker.support.TestCases.TC_FR10_03;
import static com.nikitarybalko.wargamebroker.support.TestCases.TC_FR10_04;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Selenium scenarios for the Commander Dashboard. They need the stack started with `docker compose up -d`
 * and no simulators running; the battlefield is prepared through the broker API. Subclasses choose the browser.
 */
@Tag(AbstractDashboardE2ETest.E2E_TAG)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractDashboardE2ETest {

    static final String E2E_TAG = "e2e";

    private static final String BROKER_URL = System.getProperty("e2e.broker.url", "http://localhost:8080");
    private static final int DASHBOARD_PORT = Integer.getInteger("e2e.dashboard.port", 3000);
    private static final String DASHBOARD_URL = "http://host.testcontainers.internal:" + DASHBOARD_PORT;
    private static final Duration WAIT = Duration.ofSeconds(20);

    private static final String BLUE_TANK = "blue-tank-1";
    private static final String RED_APC = "red-apc-1";
    private static final String RED_SCOUT = "red-scout-1";
    private static final long SNAPSHOT_TICK = 5;

    private static final String BLUE_FORCE = "Blue Force";
    private static final String RED_FORCE = "Red Force";
    private static final String AWAITING_TURN = "очікуємо хід";
    private static final long ALL_UNITS = 3;

    private static final String MAP = "svg[aria-label=\"Карта обстановки\"] ";
    private static final String VIEW_ZONES = MAP + "circle[stroke-dasharray=\"5 3\"]";
    private static final String FOG_HATCHING = MAP + "rect[fill*=\"hatch\"]";

    private static final String UNIT_TITLES_SCRIPT = """
            return [...document.querySelectorAll('svg[aria-label="Карта обстановки"] title')]
                .map(title => title.textContent.split('\\n')[0].split(' · ')[1]);""";

    private final BrokerApiClient broker = new BrokerApiClient(BROKER_URL);
    private BrowserWebDriverContainer<?> browser;
    private WebDriver driver;
    private WebDriverWait wait;

    protected abstract Capabilities capabilities();

    @BeforeAll
    void startBrowser() {
        assertTrue(broker.isReachable(), "Broker is not reachable at " + BROKER_URL + "; start the stack with `docker compose up -d`");
        Testcontainers.exposeHostPorts(DASHBOARD_PORT);
        browser = new BrowserWebDriverContainer<>().withCapabilities(capabilities());
        browser.start();
        driver = new RemoteWebDriver(browser.getSeleniumAddress(), capabilities());
        wait = new WebDriverWait(driver, WAIT);
    }

    @AfterAll
    void stopBrowser() {
        driver.quit();
        browser.stop();
    }

    @BeforeEach
    void prepareBattlefield() throws IOException {
        broker.startNewBattle();
        broker.playTick(1, blueForces(0), redForces(0));
        driver.get(DASHBOARD_URL);
        waitForHeader("Тік", "002");
    }

    @Test
    @Tag(TC_FR10_01)
    @DisplayName("Dashboard shows the current tick, session state and every unit on the map")
    void dashboardShowsBattlefield() {
        waitForHeader("Сесія", "активна");

        assertEquals(List.of(BLUE_TANK, RED_APC, RED_SCOUT), unitsOnMap());
        assertEquals(ALL_UNITS, countOnMap(VIEW_ZONES));
        assertEquals(AWAITING_TURN, headerValue(BLUE_FORCE));
        assertEquals(AWAITING_TURN, headerValue(RED_FORCE));
    }

    @Test
    @Tag(TC_FR10_02)
    @DisplayName("Side views show own units and only the enemies that side can see")
    void sideViewsHideUnseenEnemies() {
        selectView("Очима Blue");
        wait.until(driver -> unitsOnMap().equals(List.of(BLUE_TANK, RED_APC)));
        assertEquals(1, countOnMap(FOG_HATCHING));

        selectView("Очима Red");
        wait.until(driver -> unitsOnMap().equals(List.of(RED_APC, RED_SCOUT, BLUE_TANK)));

        selectView("Арбітр");
        wait.until(driver -> unitsOnMap().size() == ALL_UNITS);
        assertEquals(0, countOnMap(FOG_HATCHING));
    }

    @Test
    @Tag(TC_FR10_03)
    @DisplayName("Pause, resume and reset from the dashboard change the session state")
    void sessionControlsChangeSessionState() throws IOException {
        clickButton("Пауза");
        waitForHeader("Сесія", "пауза");
        assertEquals(SimulationStatus.PAUSED.name(), broker.status().path("status").asText());

        clickButton("Продовжити");
        waitForHeader("Сесія", "активна");

        clickButton("Скинути");
        clickButton("Стерти все?");
        waitForHeader("Сесія", "очікує ініціалізації");
        waitForHeader("Тік", "001");
        assertEquals(SimulationStatus.INITIALIZING.name(), broker.status().path("status").asText());
    }

    @Test
    @Tag(TC_FR10_04)
    @DisplayName("Restoring a checkpoint from the dashboard pauses the session after the snapshot tick")
    void restoreCheckpointFromDashboard() throws IOException {
        LongStream.rangeClosed(2, SNAPSHOT_TICK).forEach(this::playTickQuietly);
        waitForHeader("Тік", "006");

        By restoreButton = By.xpath("//tr[td[1][normalize-space()='005']]//button");
        wait.until(ExpectedConditions.elementToBeClickable(restoreButton)).click();
        wait.until(ExpectedConditions.textToBe(restoreButton, "Тік " + SNAPSHOT_TICK + "?"));
        driver.findElement(restoreButton).click();

        waitForHeader("Сесія", "пауза");
        waitForHeader("Тік", "006");
        assertEquals(SNAPSHOT_TICK + 1, broker.status().path("currentTick").asLong());
    }

    private void playTickQuietly(long tick) {
        try {
            broker.playTick(tick, blueForces(tick), redForces(tick));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot play tick " + tick, e);
        }
    }

    /** Blue tank at (100, 100) sees the red APC 40 units away but not the red scout in the far corner. */
    private static List<UnitDto> blueForces(long tick) {
        return List.of(unit(BLUE_TANK, Side.BLUE, UnitType.ARMOR, 100.0 + tick, 100.0, 60.0));
    }

    private static List<UnitDto> redForces(long tick) {
        return List.of(
                unit(RED_APC, Side.RED, UnitType.MECHANIZED_INFANTRY, 140.0 + tick, 100.0, 55.0),
                unit(RED_SCOUT, Side.RED, UnitType.RECON, 280.0, 180.0, 85.0));
    }

    private static UnitDto unit(String id, Side side, UnitType type, double x, double y, double viewRadius) {
        return UnitDto.builder().id(id).side(side).unitType(type).x(x).y(y).viewRadius(viewRadius)
                .status(UnitStatus.HEALTHY).build();
    }

    @SuppressWarnings("unchecked")
    private List<String> unitsOnMap() {
        return (List<String>) ((JavascriptExecutor) driver).executeScript(UNIT_TITLES_SCRIPT);
    }

    private long countOnMap(String cssSelector) {
        return driver.findElements(By.cssSelector(cssSelector)).size();
    }

    private void selectView(String label) {
        driver.findElement(By.xpath("//button[@role='radio' and normalize-space()='" + label + "']")).click();
    }

    private void clickButton(String label) {
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[normalize-space()='" + label + "']"))).click();
    }

    private String headerValue(String label) {
        return driver.findElement(headerValueLocator(label)).getText();
    }

    private void waitForHeader(String label, String value) {
        wait.until(ExpectedConditions.textToBe(headerValueLocator(label), value));
    }

    private static By headerValueLocator(String label) {
        return By.xpath("//header//p[normalize-space()='" + label + "']/following-sibling::div[1]//p");
    }
}
