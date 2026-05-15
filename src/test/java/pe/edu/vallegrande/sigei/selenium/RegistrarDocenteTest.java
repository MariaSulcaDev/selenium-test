package pe.edu.vallegrande.sigei.selenium;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TC02 — Prueba Funcional: Registro de Docente SIGEI")
class RegistrarDocenteTest extends BaseTest {

    private static final String NOMBRES = "Jorge";
    private static final String APELLIDO_PATERNO = "Castillo";
    private static final String APELLIDO_MATERNO = "Vega";
    private static final String TIPO_DOCUMENTO = "DNI";
    private static final String NUMERO_DOCUMENTO = "63251478";
    private static final String TELEFONO = "955667788";
    private static final String CORREO = "j.castillo.auto@sigei.gob.pe";
    private static final String DIRECCION = "Psje. Las Flores 210";


    @Test
    @DisplayName("Registro exitoso de docente desde panel de dirección")
    void registrarDocenteExitoso() {
        doLogin(loginUsername, loginPassword);

        wait.until(ExpectedConditions.urlContains("/direccion"));

        driver.get(baseUrl + "direccion/personal");

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h1[contains(.,'Gestion de personal')]")));

        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(.,'Nuevo docente')]")));

        driver.findElement(By.xpath("//button[contains(.,'Nuevo docente')]")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h2[normalize-space(text())='Registrar docente']")));

        WebElement inputNombres = findInputByLabel("Nombres");
        inputNombres.clear();
        inputNombres.sendKeys(NOMBRES);

        WebElement inputApellidoPaterno = findInputByLabel("Apellido paterno");
        inputApellidoPaterno.clear();
        inputApellidoPaterno.sendKeys(APELLIDO_PATERNO);

        WebElement inputApellidoMaterno = findInputByLabel("Apellido materno");
        inputApellidoMaterno.clear();
        inputApellidoMaterno.sendKeys(APELLIDO_MATERNO);

        WebElement selectDocumento = findSelectByLabel("Tipo documento");
        new Select(selectDocumento).selectByValue(TIPO_DOCUMENTO);

        WebElement inputNumeroDocumento = findInputByLabel("Numero documento");
        inputNumeroDocumento.clear();
        inputNumeroDocumento.sendKeys(NUMERO_DOCUMENTO);

        WebElement inputTelefono = findInputByLabel("Telefono");
        inputTelefono.clear();
        inputTelefono.sendKeys(TELEFONO);

        WebElement inputCorreo = findInputByLabel("Correo");
        inputCorreo.clear();
        inputCorreo.sendKeys(CORREO);

        WebElement inputDireccion = findInputByLabel("Direccion");
        inputDireccion.clear();
        inputDireccion.sendKeys(DIRECCION);

        driver.findElement(By.xpath("//button[@type='submit'][contains(.,'Guardar docente')]")).click();

        dismissSweetAlert();

        wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.xpath("//h2[normalize-space(text())='Registrar docente']")));

        WebElement tabla = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//table")));

        String contenidoTabla = tabla.getText();
        assertTrue(
                contenidoTabla.contains(APELLIDO_PATERNO) || contenidoTabla.contains(NOMBRES),
                "El docente registrado debería aparecer en la tabla");
    }
}
