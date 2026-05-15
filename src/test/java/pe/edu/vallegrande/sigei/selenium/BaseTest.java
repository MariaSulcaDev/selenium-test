package pe.edu.vallegrande.sigei.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

public abstract class BaseTest {

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected String baseUrl;
    protected String loginUsername;
    protected String loginPassword;
    protected long pauseAfterTest;

    @BeforeEach
    void setUp() throws IOException {
        Properties config = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                config.load(input);
            }
        }

        baseUrl = config.getProperty("base.url", "http://localhost:5173/SIGEI/");
        loginUsername = config.getProperty("login.username", "mauricio.torres@sigei.gob.pe");
        loginPassword = config.getProperty("login.password", "88888888");
        long timeout = Long.parseLong(config.getProperty("wait.timeout.seconds", "15"));
        pauseAfterTest = Long.parseLong(config.getProperty("pause.after.test.seconds", "4"));
        boolean headless = Boolean.parseBoolean(config.getProperty("headless", "false"));

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-popup-blocking");
        if (headless) {
            options.addArguments("--headless=new");
        }

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        Thread.sleep(pauseAfterTest * 1000);
        if (driver != null) {
            driver.quit();
        }
    }

    protected void doLogin(String username, String password) {
        driver.get(baseUrl + "login");

        WebElement usernameInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//input[@autocomplete='username']")));
        usernameInput.clear();
        usernameInput.sendKeys(username);

        WebElement passwordInput = driver.findElement(
                By.xpath("//input[@autocomplete='current-password']"));
        passwordInput.clear();
        passwordInput.sendKeys(password);

        driver.findElement(By.xpath("//button[contains(.,'Iniciar Sesión')]")).click();

        wait.until(d -> !d.getCurrentUrl().contains("/login"));
    }

    protected WebElement findInputByLabel(String labelText) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//label[normalize-space(text())='" + labelText + "']/following-sibling::div/input")));
    }

    protected WebElement findSelectByLabel(String labelText) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//label[normalize-space(text())='" + labelText + "']/following-sibling::select")));
    }

    protected void dismissSweetAlert() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".swal2-container")));
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".swal2-container")));
        } catch (Exception ignored) {
        }
    }
}
