# Firmador Segpres Hash

[![Java](https://img.shields.io/badge/Java-8-orange.svg)](https://www.oracle.com/java/)
[![gRPC](https://img.shields.io/badge/gRPC-1.52.1-blue.svg)](https://grpc.io/)
[![Maven](https://img.shields.io/badge/Maven-3.x-red.svg)](https://maven.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)

Servicio gRPC para la firma digital de documentos PDF utilizando la API de Segpres (Secretaría General de la Presidencia) de Chile. Este microservicio actúa como un puente entre aplicaciones cliente y el servicio de [firma digital de Segpres](https://firma.digital.gob.cl/), proporcionando una interfaz gRPC para el proceso de firma de documentos.

> [!IMPORTANT]
> Debes solicitar [acceso](https://firma.digital.gob.cl/como-utilizarla/) a la API de Segpres para operar este servicio.

## 📋 Tabla de Contenidos

- [Características](#-características)
- [Arquitectura](#-arquitectura)
- [Requisitos](#-requisitos)
- [Configuración](#-configuración)
- [Instalación](#-instalación)
- [Uso](#-uso)
- [API Reference](#-api-reference)
- [Estructura del Proyecto](#-estructura-del-proyecto)
- [Tecnologías](#-tecnologías)
- [Desarrollo](#-desarrollo)
- [Docker](#-docker)
- [Monitoreo](#-monitoreo)
- [Contribución](#-contribución)
- [Licencia](#-licencia)
- [Soporte](#-soporte)

## ✨ Características

- **Firma Digital PDF**: Firma documentos PDF usando certificados digitales de Segpres
- **Soporte para Firmas Visibles e Invisibles**: Configurable según necesidades
- **Autenticación JWT**: Sistema de tokens para autenticación segura
- **Encriptación RSA**: Las contraseñas se envían encriptadas con RSA/PKCS1
- **Firma Atendida y Desatendida**: Soporte para ambos modos de operación
- **Monitoreo con Elastic APM**: Trazabilidad completa de transacciones
- **Reintentos Automáticos**: Manejo inteligente de errores con reintentos
- **Contenedorización**: Imagen Docker lista para producción
- **Configuración Flexible**: Via variables de entorno

## 🏗️ Arquitectura

```
┌─────────────────┐    gRPC     ┌──────────────────┐    HTTPS    ┌─────────────────┐
│   Cliente       │ ──────────► │   gRPC Server    │ ──────────► │   Segpres API   │
│   (Aplicación)  │             │   (Este servicio)│             │                 │
└─────────────────┘             └──────────────────┘             └─────────────────┘
```

### Flujo de Firma

1. **Recepción**: El cliente envía una solicitud gRPC con el PDF y metadatos
2. **Procesamiento**: Se genera el hash SHA-256 del documento
3. **Autenticación**: Se crea un JWT con las credenciales del usuario
4. **Solicitud a Segpres**: Se envía el hash a la API de Segpres para firmar
5. **Respuesta**: Se retorna el PDF firmado al cliente

## 📋 Requisitos

### Requisitos del Sistema
- **Java**: OpenJDK 8 o superior
- **Maven**: 3.6 o superior
- **Memoria**: Mínimo 512MB RAM
- **Red**: Acceso a la API de Segpres
- **Protobuf**: Para la generación del código gRPC

### Dependencias Principales
- gRPC Java 1.52.1
- iText PDF 5.5.13.3
- Bouncy Castle Crypto 1.70
- Apache HttpClient 4.5.14
- Elastic APM 1.35.0

## ⚙️ Configuración

### Variables de Entorno Requeridas

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `SEGPRES_API_TOKEN_KEY` | Token de API de Segpres | N/A |
| `SEGPRES_SECRET` | Secreto para firma | N/A |
| `SEGPRES_BASE_URL` | URL base de la API de Segpres | `https://api.firma.cert.digital.gob.cl` |

### Variables de Entorno Opcionales

| Variable | Descripción | Valor por Defecto | Rango |
|----------|-------------|-------------------|-------|
| `APP_LOGGING_LEVEL` | Nivel de log | `INFO` | `TRACE,DEBUG,INFO,WARN,ERROR` |
| `APP_TIMEZONE` | Zona horaria | Sistema | `America/Santiago` |
| `APP_TIMEOUT` | Timeout de conexión (en milisegundos) | `60000` (60 segundos) | `10000-300000` (10-300 segundos) |
| `APP_THREADS` | Número de hilos del servidor | `5` | `1-20` |
| `APP_MAX_INBOUND_MESSAGE_SIZE` | Tamaño máximo de mensaje (en bytes) | `4194304` (4 MB) | `1048576-104857600` (1-100 MB) |

### Configuración de Certificados

El servicio requiere una clave privada RSA para descifrar las contraseñas:

```bash
# Generar la clave privada RSA (2048 bits)
# Esta clave se usará para descifrar las contraseñas
# Asegúrate de proteger esta clave y no compartirla públicamente
openssl genrsa -out private.pem 2048

# Generar la clave pública RSA desde la clave privada
# Esta clave se usará para cifrar las contraseñas antes de enviarlas
# al servicio gRPC
# Asegúrate de que la clave pública esté disponible para el cliente
# que enviará las contraseñas encriptadas
openssl rsa -in private.pem -pubout -out public.pem

# Crear directorio de secretos
mkdir -p secret

# Mover la clave privada
mv private.pem secret/private.pem
```

### Generación de Código Proto

> [!NOTE]
> Para generar los archivos es necesario instalar [protobuf](https://github.com/protocolbuffers/protobuf).

1. Descargar el [plugin](https://repo1.maven.org/maven2/io/grpc/protoc-gen-grpc-java/1.52.1):

```bash
wget https://repo1.maven.org/maven2/io/grpc/protoc-gen-grpc-java/1.52.1/protoc-gen-grpc-java-1.52.1-linux-x86_64.exe
```

2. Otorgarle permisos de ejecución al plugin:

```bash
chmod +x protoc-gen-grpc-java-1.52.1-linux-x86_64.exe
```

3. Generar los archivos:

```bash
protoc \
  --java_out=src/main/java \
  --grpc-java_out=src/main/java \
  --plugin=protoc-gen-grpc-java=protoc-gen-grpc-java-1.52.1-linux-x86_64.exe \
  signerGRPC.proto
```

## 🚀 Instalación

### Desde el Código Fuente

```bash
# Clonar el repositorio
git clone https://github.com/vti-uchile/firmador-segpres-hash
cd firmador-segpres-hash

# Compilar el proyecto
mvn clean package

# El JAR se genera en target/firmador-segpres-hash-1.0.0-jar-with-dependencies.jar
```

### Con Docker

```bash
# Construir la imagen
docker build -t firmador-segpres-hash .

# Cambiar propietario del directorio de secretos
sudo chown -R 1001:1001 secret

# Ejecutar el contenedor
docker run --rm -it \
  -p 8080:8080 \
  -e SEGPRES_API_TOKEN_KEY="<api-token-key>" \
  -e SEGPRES_SECRET="<secret>" \
  -e SEGPRES_BASE_URL=https://api.firma.cert.digital.gob.cl \
  -e ELASTIC_APM_ENABLED=false \
  -v ./secret:/app/secret \
  firmador-segpres-hash
```

## 🔧 Uso

### Cliente gRPC (Ejemplo en Go)

> [!NOTE]
> Para generar los archivos es necesario instalar [protobuf](https://github.com/protocolbuffers/protobuf).

1. Instalar los plugins:

```bash
go install google.golang.org/protobuf/cmd/protoc-gen-go@v1.28.0
go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@v1.2.0
```

2. Establecer en la variable de entorno la ruta donde se instalaron los plugins:

```bash
export PATH="$PATH:$(go env GOPATH)/bin"
```

3. Generar los archivos:

```bash
mkdir -p examples/go/lib/proto
protoc \
  --go_out=examples/go/lib/proto --go_opt=paths=source_relative \
  --go-grpc_out=examples/go/lib/proto --go-grpc_opt=paths=source_relative \
  signerGRPC.proto
```

4. Ejecutar el cliente:

```bash
# Copiar la clave pública al directorio del cliente
cp public.pem examples/go/public.pem

# Ir al directorio del cliente
cd examples/go

# Ejecutar el cliente
# El archivo por defecto es document.pdf, puedes modificarlo mediante el argumento -filename
# Para obtener más información puedes utilizar el argumento -help
go run main.go -rut 12345678-9
```

5. Confirmar que el archivo generado se encuentre firmado:

```bash
pdfsig signed-document-1.pdf
```

## 📖 API Reference

### Servicio: `signerGRPC.Signer`

#### Método: `Send`

**Request: `SignRequest`**

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `name` | `string` | Nombre del archivo |
| `file` | `bytes` | Datos del archivo PDF a firmar |
| `signature` | `bytes` | Imagen de firma (PNG/JPG) |
| `rut` | `string` | RUT del firmante |
| `password` | `string` | OTP encriptado con RSA |
| `page` | `int32` | Página para firma visible |
| `llx` | `int32` | Coordenada X inferior izquierda |
| `lly` | `int32` | Coordenada Y inferior izquierda |
| `urx` | `int32` | Coordenada X superior derecha |
| `ury` | `int32` | Coordenada Y superior derecha |
| `attended` | `bool` | Firma atendida (true) o desatendida (false) |

**Response: `SignReply`**

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `success` | `bool` | Indica si la firma fue exitosa |
| `file` | `bytes` | Datos del archivo PDF firmado |
| `message` | `string` | Mensaje descriptivo del resultado |
| `invalid_password` | `bool` | Indica si la contraseña/OTP es inválida |
| `retry` | `bool` | Indica si se debe reintentar la operación |

## 📁 Estructura del Proyecto

```
firmador-segpres-hash/
├── src/main/java/cl/uchile/fea/
│   ├── App.java                    # Clase principal
│   ├── Utils.java                  # Utilidades generales
│   ├── HttpStatusCode.java         # Códigos de estado HTTP
│   ├── grpc/
│   │   ├── SignerServer.java       # Servidor gRPC
│   │   ├── SignerService.java      # Implementación del servicio
│   │   └── SignException.java      # Excepción personalizada
│   ├── jwt/
│   │   └── JwtUtil.java            # Utilidades JWT
│   └── segpres/
│       ├── SegpresService.java     # Cliente API Segpres
│       ├── LayoutUtil.java         # Generación de layouts XML
│       ├── CustomHttpResponse.java # Respuesta HTTP personalizada
│       └── models/                 # Modelos de datos
│           ├── SignatureRequest.java
│           ├── SignatureResponse.java
│           ├── HashRequest.java
│           ├── HashResponse.java
│           ├── Metadata.java
│           └── ErrorResponse.java
├── src/main/resources/
│   └── logback.xml                 # Configuración de logging
├── signerGRPC.proto                # Definición del servicio gRPC
├── pom.xml                         # Configuración Maven
├── Dockerfile                      # Imagen Docker
├── LICENSE                         # Licencia del proyecto
└── README.md                       # Este archivo
```

## 🛠️ Tecnologías

### Frameworks y Librerías

- **[gRPC](https://grpc.io/)**: Framework de comunicación RPC
- **[Protocol Buffers](https://developers.google.com/protocol-buffers)**: Serialización de datos
- **[iText](https://itextpdf.com/)**: Manipulación de archivos PDF AGPLV3
- **[Bouncy Castle](https://www.bouncycastle.org/)**: Criptografía y certificados
- **[Apache HttpClient](https://hc.apache.org/)**: Cliente HTTP
- **[Gson](https://github.com/google/gson)**: Serialización JSON
- **[SLF4J + Logback](http://www.slf4j.org/)**: Sistema de logging
- **[Elastic APM](https://www.elastic.co/apm/)**: Monitoreo de aplicaciones

### Herramientas de Desarrollo

- **Java 8**: Lenguaje de programación
- **Maven 3**: Gestión de dependencias y construcción
- **Docker**: Contenedorización
- **Protocol Buffer Compiler**: Generación de código desde .proto

## 🔨 Desarrollo

### Configuración del Entorno

```bash
# Instalar dependencias
mvn clean install

# Ejecutar en modo desarrollo
mvn exec:java -Dexec.mainClass="cl.uchile.fea.App"
```

### Variables de Desarrollo

```bash
APP_LOGGING_LEVEL=DEBUG
# APP_TIMEZONE=America/Santiago
APP_TIMEOUT=30000
# APP_THREADS=5
# APP_MAX_INBOUND_MESSAGE_SIZE=4194304

SEGPRES_API_TOKEN_KEY="<api-token-key>"
SEGPRES_SECRET="<secret>"
SEGPRES_BASE_URL=https://api.firma.cert.digital.gob.cl
```

## 🐳 Docker

### Construcción de la Imagen

```bash
# Construcción estándar
docker build -t firmador-segpres-hash:latest .

# Construcción con argumentos
docker build \
  --build-arg MAVEN_OPTS="-Xmx1024m" \
  -t firmador-segpres-hash:latest .
```

### Ejecución con Docker Compose

```yaml
# docker-compose.yml
version: '3.8'
services:
  grpc-signer:
    image: firmador-segpres-hash:latest
    ports:
      - "8080:8080"
    environment:
      - APP_LOGGING_LEVEL=INFO
      # - APP_TIMEZONE=America/Santiago
      # - APP_TIMEOUT=60000
      - APP_THREADS=10
      # - APP_MAX_INBOUND_MESSAGE_SIZE=4194304
      - SEGPRES_API_TOKEN_KEY=${SEGPRES_API_TOKEN_KEY}
      - SEGPRES_SECRET=${SEGPRES_SECRET}
      - SEGPRES_BASE_URL=${SEGPRES_BASE_URL}
      - ELASTIC_APM_ENABLED=false
    volumes:
      - ./secret:/app/secret:ro
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "grpc_health_probe", "-addr=:8080"]
      interval: 30s
      timeout: 10s
      retries: 3
```

### Despliegue en Kubernetes

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: firmador-segpres-hash
spec:
  replicas: 3
  selector:
    matchLabels:
      app: firmador-segpres-hash
  template:
    metadata:
      labels:
        app: firmador-segpres-hash
    spec:
      containers:
      - name: firmador-segpres-hash
        image: firmador-segpres-hash:latest
        ports:
        - containerPort: 8080
        env:
        - name: SEGPRES_API_TOKEN_KEY
          valueFrom:
            secretKeyRef:
              name: segpres-secrets
              key: api-token
        - name: SEGPRES_SECRET
          valueFrom:
            secretKeyRef:
              name: segpres-secrets
              key: jwt-secret
        - name: SEGPRES_BASE_URL
          value: "https://api.firma.cert.digital.gob.cl"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        volumeMounts:
        - name: rsa-keys
          mountPath: /app/secret
          readOnly: true
      volumes:
      - name: rsa-keys
        secret:
          secretName: rsa-private-key
```

## 📊 Monitoreo

### Elastic APM

El servicio incluye integración con Elastic APM para monitoreo:

- **Transacciones**: Cada solicitud de firma es trazada
- **Spans**: Operaciones internas como generación de PDF y llamadas HTTP
- **Errores**: Excepciones y errores son reportados automáticamente
- **Métricas**: Performance y uso de recursos

### Configuración APM

```bash
# https://www.elastic.co/guide/en/apm/agent/java/current/configuration.html
ELASTIC_APM_SERVICE_NAME=firmador-segpres-hash
ELASTIC_APM_SERVER_URLS=https://apm.example.com:8200
# ELASTIC_APM_SECRET_TOKEN=""
ELASTIC_APM_ENVIRONMENT=production
# https://www.elastic.co/guide/en/apm/agent/java/current/config-stacktrace.html#config-application-packages
ELASTIC_APM_APPLICATION_PACKAGES=cl.uchile.fea
# https://www.elastic.co/guide/en/apm/agent/java/current/config-core.html#config-cloud-provider
ELASTIC_APM_CLOUD_PROVIDER=NONE
```

## 🤝 Contribución

### Guías para Contribuir

1. **Fork** el repositorio
2. **Crear** una rama para tu feature (`git checkout -b feature/nueva-funcionalidad`)
3. **Commit** tus cambios (`git commit -am 'Agregar nueva funcionalidad'`)
4. **Push** a la rama (`git push origin feature/nueva-funcionalidad`)
5. **Crear** un Pull Request

### Estándares de Código

- **Java Code Style**: Seguir las convenciones de Oracle
- **Javadoc**: Documentar todas las clases y métodos públicos
- **Tests**: Incluir tests unitarios para nuevas funcionalidades
- **Logs**: Usar niveles apropiados (TRACE, DEBUG, INFO, WARN, ERROR)

### Reportar Issues

Al reportar problemas, incluye:

- **Versión** del servicio
- **Configuración** (variables de entorno, sin secretos)
- **Logs** relevantes
- **Pasos** para reproducir el error
- **Comportamiento esperado** vs actual

## 📄 Licencia

Este proyecto está bajo la licencia AGPLv3 heredada de [itextpdf](https://itextpdf.com/how-buy/AGPLv3-license).

## 📞 Soporte

Para soporte técnico:

- **Email**: [arquitectura-vti@uchile.cl](mailto:arquitectura-vti@uchile.cl)
- **Documentación**: Consulta la documentación interna de Segpres
- **Issues**: Usa el sistema de issues del repositorio

---

- **Desarrollado por**: VTI - Universidad de Chile
  - [Manuel Alba](https://github.com/elmalba) <[manuel.alba@uchile.cl](mailto:manuel.alba@uchile.cl)>
  - Pablo De la Cruz <[pablo.delacruz@uchile.cl](mailto:pablo.delacruz@uchile.cl)>
- **Mantenido por**: Equipo Arquitectura
- **Versión**: 1.0.0
