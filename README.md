# vg-ms-users-management

## 1. Implementación del Pipeline en Jenkins

Este proyecto Java utiliza Maven como gestor de compilación y pruebas. El pipeline de Jenkins está diseñado para:

- Clonar el repositorio
- Compilar el código
- Ejecutar pruebas unitarias
- Generar el artefacto final
- Enviar notificaciones a Slack
- Integrar análisis de calidad con SonarQube

### Jenkinsfile de ejemplo

```groovy
pipeline {
    agent any
    tools {
        maven 'MAVEN'
    }
    environment {
        GIT_TOKEN = 'glpat-1G6Fnx82EJvXC00PYljZ32M6MQpvOjEKdTppNW9uYg8.01.171ox0434'
        REPO_PATH = 'gitlab.com/vallegrande/as232s5_prs1/vg-ms-users-management.git'
        SONAR_TOKEN = credentials('SONAR_TOKEN')
    }
    stages {
        stage('Checkout') {
            steps {
                deleteDir()
                sh "git clone https://oauth2:${env.GIT_TOKEN}@${env.REPO_PATH} ."
                sh 'git checkout main'
            }
        }
        stage('Build & Test') {
            steps {
                sh 'mvn clean test'
            }
        }
        stage('SonarQube Analysis') {
            steps {
                sh 'mvn sonar:sonar -Dsonar.login=${SONAR_TOKEN} -Dsonar.host.url=https://sonarcloud.io -Dsonar.organization=as232s5-prs1 -Dsonar.projectKey=as232s5-prs1_vg-ms-users-management'
            }
        }
    }
    post {
        success {
            slackSend(color: 'good', message: "Build Exitoso: ${env.JOB_NAME}")
        }
        failure {
            slackSend(color: 'danger', message: "Build Fallido: ${env.JOB_NAME}")
        }
    }
}
```

> Nota: este pipeline usa `mvn sonar:sonar -Dsonar.login=${SONAR_TOKEN}` directamente para SonarCloud. Esto evita `withSonarQubeEnv`, que no está disponible si el plugin de SonarQube no está instalado en Jenkins.
>
> También se recomienda usar un `Credential` de tipo secret text en Jenkins llamado `SONAR_TOKEN` para no exponer el token directamente en el pipeline.
>
> El archivo `sonar-project.properties` del proyecto contiene la configuración de SonarCloud.
>
> ```properties
> sonar.projectKey=as232s5-prs1_vg-ms-users-management
> sonar.organization=as232s5-prs1
> sonar.host.url=https://sonarcloud.io
> sonar.token=${SONAR_TOKEN}
>
> sonar.sources=src
> sonar.java.binaries=target
> sonar.sourceEncoding=UTF-8
> ```

### Detalles del pipeline

- `agent any`: permite ejecutar el pipeline en cualquier agente disponible.
- `tools { maven 'MAVEN' }`: usa la instalación de Maven llamada `MAVEN` en Jenkins.
- `sh 'mvn clean test'`: limpia el proyecto y corre pruebas.
- `sh 'mvn sonar:sonar -Dsonar.login=${SONAR_TOKEN} -Dsonar.host.url=https://sonarcloud.io -Dsonar.organization=as232s5-prs1 -Dsonar.projectKey=as232s5-prs1_vg-ms-users-management'`: analiza el código en SonarCloud.
- `slackSend(...)`: envía notificaciones de éxito o fallo a Slack.

## 1.1 Evidencias del pipeline y envíos a Slack

- Build `#14` en Jenkins se completó con éxito.
- El pipeline ejecutó `mvn clean test` y corrió 105 pruebas unitarias sin fallas.
- Se generó el reporte de cobertura de JaCoCo y el análisis de SonarCloud.
- La notificación de Slack se envió correctamente al canal `notificaciones-dev`.

![1778798764003](image/README/1778798764003.png)

## 2. Integración de Slack con Jenkins

### Qué se hace

Se integra Jenkins con Slack para recibir alertas automáticas del estado del pipeline. Así se puede saber de inmediato si la compilación pasó o falló.

### Cómo se configura

1. En Jenkins, instalar el plugin `Slack Notification`.
2. Configurar la integración global en `Manage Jenkins > Configure System > Slack`.
3. Usar un `Workspace URL` válido y el `Credential` del token.
4. En el pipeline, utilizar el paso `slackSend`.

### Ejemplo de configuración

- Workspace: `https://jenkinsmaria.slack.com`
- Credential: `slack`
- Canal predeterminado: `notificaciones-dev`
- Bot de Slack: `Custom slack app bot user`

### Resultado esperado

- En caso de `success`, se envía:
  - `Build Exitoso: <JOB_NAME>`
- En caso de `failure`, se envía:
  - `Build Fallido: <JOB_NAME>`

> Agregar captura de pantalla de la configuración de Slack en Jenkins aquí.

## 3. Pruebas de carga o estrés con JMeter

### Archivo de prueba usado

- `users_load_test.jmx`

### Objetivo

Validar el comportamiento del servicio `vg-ms-users-management` bajo diferentes niveles de carga:

- Verde: carga normal
- Amarillo: carga moderada
- Rojo: estrés máximo

### Escenarios implementados

#### Verde - Carga Normal (10 usuarios)

- Usuarios concurrentes: 10
- Actividad típica:
  - Obtener usuarios por rol y estado
  - Obtener usuarios por institución
  - Crear usuarios con datos aleatorios
- Propósito: verificar el funcionamiento correcto con carga moderada.

#### Amarillo - Carga Moderada (50 usuarios)

- Usuarios concurrentes: 50
- Actividad típica:
  - Listar usuarios
  - Crear usuarios
  - Consultar usuarios activos por rol
  - Consultar usuarios por institución
- Propósito: identificar degradación de rendimiento antes de una carga alta.

#### Rojo - Estrés Máximo (200 usuarios)

- Usuarios concurrentes: 200
- Duración: 180 segundos
- Actividad típica:
  - Listar todos los usuarios
  - Crear usuarios de prueba en línea
  - Consultar usuarios por institución
  - Consultar directores activos
- Propósito: validar estabilidad y detección de fallos bajo máxima carga.

### Resultados y análisis esperado

Para cada escenario, se deben interpretar los siguientes indicadores:

- `Throughput`: cantidad de peticiones procesadas por segundo.
- `Latency / Tiempo de respuesta`: si se mantiene aceptable en Verde y Amarillo.
- `Error %`: debe ser cercano a 0 en Verde y Amarillo; en Rojo puede subir, pero no debe haber fallas críticas continuas.
- `Tiempo de respuesta máximo`: en Verde debe ser bajo; en Amarillo debe subir de forma controlada; en Rojo puede ser mayor por saturación.

#### Interpretación general

- Si el escenario Verde responde con baja latencia y 0% errores, el sistema está bien dimensionado para uso normal.
- Si el escenario Amarillo mantiene estabilidad y no hay errores, hay margen de crecimiento.
- Si el escenario Rojo muestra errores o tiempos excesivos, se debe mejorar la arquitectura, la base de datos o la escalabilidad.

## 3.1 Resultados reales de JMeter

Los resultados obtenidos en el archivo `users_load_test.jmx` muestran lo siguiente:

- Verde: 196 muestras totales, 2.8 peticiones/sec, promedio ~2958 ms, error ~15.3%.
- Amarillo: 777 muestras totales, 6.0 peticiones/sec, promedio ~7406 ms, error ~46.7%.
- Rojo: 2931 muestras totales, 22.8 peticiones/sec, promedio ~7732 ms, error ~55.2%.

Esto indica que el sistema responde con tiempos aceptables en Verde, pero se degrada significativamente en Amarillo y Rojo. Los errores en Amarillo/Rojo sugieren que las rutas `POST /api/users` y las consultas por institución/rol necesitan optimización para carga alta.

![JMeter Resumen Verde](image/README/1778796261290.png)

![JMeter Resumen Amarillo](image/README/1778796285989.png)

![JMeter Resumen Rojo](image/README/1778796301413.png)
> Añade estas capturas en tu guía de investigación para evidenciar los niveles de carga y los resultados obtenidos.

## 4. Pruebas funcionales con Selenium

### ¿Qué son?

Las pruebas funcionales verifican que el sistema hace lo que debe hacer desde la perspectiva del usuario. Se centra en el comportamiento de la aplicación frente a entradas reales y verifica que las funcionalidades principales funcionen correctamente.

### ¿Por qué realizar pruebas funcionales?

- Aseguran que la aplicación cumple con los requisitos.
- Detectan errores en el flujo de trabajo del usuario.
- Mejoran la calidad y confiabilidad del sistema.
- Evitan regresiones cuando se hacen cambios en el código.

### Ventajas

- Simulan el comportamiento real del usuario.
- Permiten automatizar la verificación de flujos críticos.
- Ayudan a detectar fallos antes de la entrega.
- Son útiles para pruebas de regresión y para mantener la calidad.

### Herramientas recomendadas

#### Gratuitas

- Selenium WebDriver: framework de automatización de navegación y acciones de usuario en el navegador.
- Katalon Recorder / Katalon Studio Free Edition: solución gratuita para grabar y ejecutar pruebas funcionales.

#### De pago

- TestComplete: plataforma de automatización para aplicaciones web y de escritorio.
- Ranorex Studio: herramienta para pruebas UI con soporte para múltiples tecnologías.

## 5. Propuestas de pruebas funcionales para este proyecto

### Propuesta 1: Registro de usuario

Objetivo: validar que el registro de un nuevo usuario funciona correctamente.

Pasos:

1. Abrir la pantalla de creación de usuario en la interfaz del proyecto.
2. Ingresar todos los campos obligatorios: institución, nombre, apellido, tipo de documento, número de documento, teléfono, dirección, email y rol.
3. Enviar el formulario.
4. Verificar que el sistema devuelve un mensaje de éxito y que el usuario aparece en la lista de usuarios.

Criterios de aceptación:

- El formulario debe aceptar datos válidos.
- La API debe crear el usuario sin error.
- El nuevo usuario debe estar disponible en la consulta `/api/users/institution/{institutionId}`.

### Propuesta 2: Consulta de usuarios activos por rol

Objetivo: verificar que la búsqueda de usuarios activos por rol funcione correctamente.

Pasos:

1. Navegar a la función de búsqueda por rol.
2. Seleccionar el rol `DOCENTE`.
3. Ejecutar la consulta.
4. Confirmar que la lista resultante sólo contiene usuarios con estado `ACTIVE` y rol `DOCENTE`.

Criterios de aceptación:

- La petición `GET /api/users/role/DOCENTE/status/ACTIVE` debe responder correctamente.
- La lista debe mostrar datos coherentes del usuario.
- No debe haber errores de autenticación ni de servidor.

---

## 6. Implementación de pruebas de integración frontend con Selenium

> **Guión para video — Pruebas de integración frontend + microservicio de usuarios**

---

### 6.1 ¿De qué van estas pruebas?

Este bloque valida la **integración real entre el frontend React (SIGEI) y el microservicio `vg-ms-users-management`**. No se prueban solo endpoints REST en aislamiento; se prueba el flujo completo que haría un usuario real en el navegador:

1. Abrir el sistema en `http://localhost:5173/SIGEI/`
2. Iniciar sesión con credenciales reales
3. Registrar un docente desde el panel de dirección

Si alguno de esos pasos falla, la prueba falla. Eso significa que el microservicio, la autenticación, el gateway y el frontend están correctamente conectados y funcionando juntos.

---

### 6.2 Stack tecnológico del proyecto de pruebas

| Componente | Tecnología | Versión |
| --- | --- | --- |
| Lenguaje | Java | 17 |
| Build tool | Maven | 3.x |
| Automatización de navegador | Selenium WebDriver | 4.21.0 |
| Gestión de ChromeDriver | WebDriverManager (bonigarcia) | 5.8.0 |
| Framework de pruebas | JUnit Jupiter | 5.10.2 |
| Logging | SLF4J Simple | 2.0.13 |

El proyecto vive en `demos/selenium-sigei/` dentro del mismo repositorio.

---

### 6.3 Estructura del proyecto

```text
demos/selenium-sigei/
├── pom.xml
└── src/
    └── test/
        ├── java/pe/edu/vallegrande/sigei/selenium/
        │   ├── BaseTest.java              ← clase base con ciclo de vida del driver
        │   ├── LoginTest.java             ← TC01: 3 casos de prueba de login
        │   └── RegistrarDocenteTest.java  ← TC02: registro de docente desde UI
        └── resources/
            └── config.properties          ← URL, credenciales, timeouts
```

---

### 6.4 Cómo se construyó (paso a paso)

#### Paso 1 — Clase base `BaseTest`

Se creó una clase abstracta que maneja el ciclo de vida del navegador:

- `@BeforeEach setUp()`: lee `config.properties`, descarga y configura ChromeDriver automáticamente con WebDriverManager, abre Chrome maximizado sin notificaciones.
- `@AfterEach tearDown()`: espera `pause.after.test.seconds` segundos (por defecto 4) para que se pueda observar el resultado en pantalla, luego cierra el navegador.
- Métodos utilitarios compartidos: `doLogin()`, `findInputByLabel()`, `findSelectByLabel()`, `dismissSweetAlert()`.

El uso de `WebDriverManager` elimina la necesidad de descargar manualmente el binario de ChromeDriver. Se gestiona solo.

#### Paso 2 — Configuración externalizada

Toda la configuración sensible y variable vive en `config.properties`:

```properties
base.url=http://localhost:5173/SIGEI/
login.username=mauricio.torres@sigei.gob.pe
login.password=88888888
wait.timeout.seconds=15
headless=false
pause.after.test.seconds=4
pause.between.suites.seconds=6
```

Esto permite cambiar la URL o las credenciales sin tocar el código Java.

#### Paso 3 — TC01: LoginTest (3 casos)

Se automatizaron tres escenarios de login:

| # | Nombre | Qué valida |
| --- | --- | --- |
| 1 | Login exitoso | Ingresa con credenciales correctas → redirige al dashboard `/direccion` |
| 2 | Contraseña incorrecta | Ingresa contraseña inválida → permanece en `/login` con alerta |
| 3 | Campos vacíos | Hace clic en "Iniciar Sesión" sin datos → permanece en `/login` con alerta |

El caso 1 verifica que la URL cambie a `/direccion` (rol DIRECTOR), lo que confirma que:

- El frontend consumió correctamente la API de autenticación
- El microservicio validó las credenciales
- El token JWT fue procesado y se asignó el rol

#### Paso 4 — TC02: RegistrarDocenteTest

Prueba de extremo a extremo completa:

1. Hace login con `mauricio.torres@sigei.gob.pe`
2. Espera que la URL contenga `/direccion`
3. Navega a `direccion/personal`
4. Hace clic en el botón **"Nuevo docente"**
5. Llena todos los campos del modal:
   - Nombres: `Valeria`
   - Apellido paterno: `Navarro`
   - Apellido materno: `Salas`
   - Tipo documento: `DNI`
   - Número de documento: `78945612`
   - Teléfono: `944556677`
   - Correo: `v.navarro.selenium@sigei.gob.pe`
   - Dirección: `Av. Central 999`
6. Hace clic en **"Guardar docente"**
7. Espera que la alerta de SweetAlert2 desaparezca
8. Espera que el modal se cierre
9. **Verifica que `Navarro` o `Valeria` aparezca en la tabla de personal**

Si la fila aparece en la tabla, confirma que el microservicio `vg-ms-users-management` creó el usuario correctamente y el frontend lo refleja sin necesidad de recargar.

---

### 6.5 Flujo de ejecución al correr `mvn test`

```text
[ TC01 — LoginTest ]
  ├─ Chrome abre → Login exitoso   → espera 4s → Chrome cierra
  ├─ Chrome abre → Contraseña mal  → espera 4s → Chrome cierra
  ├─ Chrome abre → Campos vacíos   → espera 4s → Chrome cierra
  └─ @AfterAll   → espera 6s (pausa visible entre suites)

[ TC02 — RegistrarDocenteTest ]
  └─ Chrome abre → Login + Registrar docente → espera 4s → Chrome cierra
```

Cada prueba abre su propio navegador de forma independiente. Así se garantiza aislamiento total entre casos de prueba.

---

### 6.6 Cómo ejecutar las pruebas

**Requisitos previos:**

- Tener el frontend corriendo en `http://localhost:5173/SIGEI/` (`npm run dev` en `vg-web-sigei`)
- Tener todos los microservicios levantados (pueden usarse con `docker-compose`)
- Tener Google Chrome instalado
- Tener Java 17 y Maven instalados

**Comando:**

```bash
cd demos/selenium-sigei
mvn test
```

Los resultados se muestran en consola. Maven Surefire genera el reporte en `target/surefire-reports/`.

---

### 6.7 Relación directa con `vg-ms-users-management`

| Acción de la prueba | Endpoint del microservicio |
| --- | --- |
| Iniciar sesión | `POST /api/auth/login` (a través del gateway) |
| Obtener datos del usuario logueado | `GET /api/users/email/{email}` |
| Registrar docente desde el modal | `POST /api/users` |
| Listar docentes en la tabla de personal | `GET /api/users/institution/{id}/role/DOCENTE` |

La prueba valida que toda esa cadena funcione de forma integrada: **frontend → gateway → microservicio → base de datos → respuesta reflejada en la tabla**.
