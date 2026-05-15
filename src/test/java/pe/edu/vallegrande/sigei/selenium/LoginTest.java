package pe.edu.vallegrande.sigei.selenium;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TC01 — Prueba Funcional: Login SIGEI")
class LoginTest extends BaseTest {

    @AfterAll
    static void esperarAntesDeRegistroDocente() throws InterruptedException, IOException {
        Properties config = new Properties();
        try (InputStream input = LoginTest.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input != null) {
                config.load(input);
            }
        }
        long pause = Long.parseLong(config.getProperty("pause.between.suites.seconds", "6"));
        Thread.sleep(pause * 1000);
    }

    @Test
    @DisplayName("Login exitoso con credenciales válidas redirige al dashboard")
    void loginExitosoRedirigeAlDashboard() {
        driver.get(baseUrl + "login");

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h2[contains(.,'Bienvenido')]")));

        WebElement usernameInput = driver.findElement(
                By.xpath("//input[@autocomplete='username']"));
        usernameInput.sendKeys(loginUsername);

        driver.findElement(By.xpath("//input[@autocomplete='current-password']"))
                .sendKeys(loginPassword);

        driver.findElement(By.xpath("//button[contains(.,'Iniciar Sesión')]")).click();

        dismissSweetAlert();

        wait.until(d -> !d.getCurrentUrl().contains("/login"));

        String currentUrl = driver.getCurrentUrl();
        assertFalse(currentUrl.contains("/login"),
                "Debería haber salido de la página de login");
        assertTrue(
                currentUrl.contains("/direccion") ||
                        currentUrl.contains("/admin") ||
                        currentUrl.contains("/secretaria") ||
                        currentUrl.contains("/docente"),
                "Debería haber navegado al dashboard según el rol del usuario");
    }

    @Test
    @DisplayName("Login fallido con contraseña incorrecta mantiene en login")
    void loginFallidoConContrasenaIncorrectaMantieneEnLogin() {
        driver.get(baseUrl + "login");

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//input[@autocomplete='username']")));

        driver.findElement(By.xpath("//input[@autocomplete='username']"))
                .sendKeys(loginUsername);

        driver.findElement(By.xpath("//input[@autocomplete='current-password']"))
                .sendKeys("contraseña_invalida_9999");

        driver.findElement(By.xpath("//button[contains(.,'Iniciar Sesión')]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector(".swal2-container")));

        assertTrue(driver.getCurrentUrl().contains("/login"),
                "Debería permanecer en la página de login al fallar la autenticación");
    }

    @Test
    @DisplayName("Login fallido con campos vacíos muestra advertencia")
    void loginFallidoConCamposVacios() {
        driver.get(baseUrl + "login");

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//button[contains(.,'Iniciar Sesión')]")));

        driver.findElement(By.xpath("//button[contains(.,'Iniciar Sesión')]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector(".swal2-container")));

        assertTrue(driver.getCurrentUrl().contains("/login"),
                "Debería permanecer en login cuando los campos están vacíos");
    }
}
