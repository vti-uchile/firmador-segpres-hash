# gRPC Segpres Hash Signer

[![Java](https://img.shields.io/badge/Java-8-orange.svg)](https://www.oracle.com/java/)
[![gRPC](https://img.shields.io/badge/gRPC-1.52.1-blue.svg)](https://grpc.io/)
[![Maven](https://img.shields.io/badge/Maven-3.x-red.svg)](https://maven.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)

Servicio gRPC para la firma digital de documentos PDF utilizando la API de Segpres (Secretaría General de la Presidencia) de Chile. Este microservicio actúa como un puente entre aplicaciones cliente y el servicio de firma digital de Segpres, proporcionando una interfaz gRPC para el proceso de firma de documentos.

## 📋 Tabla de Contenidos

- [Características](#-características)
- [Arquitectura](#-arquitectura)
- [Requisitos](#-requisitos)
- [Instalación](#-instalación)
- [Configuración](#-configuración)
- [Uso](#-uso)
- [API Reference](#-api-reference)
- [Estructura del Proyecto](#-estructura-del-proyecto)
- [Tecnologías](#-tecnologías)
- [Desarrollo](#-desarrollo)
- [Docker](#-docker)
- [Monitoreo](#-monitoreo)
- [Contribución](#-contribución)
- [Licencia](#-licencia)

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
4. **Solicitud a Segpres**: Se envía el hash a la API de Segpres para firma
5. **Respuesta**: Se retorna el PDF firmado al cliente

## 📋 Requisitos

### Requisitos del Sistema
- **Java**: OpenJDK 8 o superior
- **Maven**: 3.6 o superior
- **Memoria**: Mínimo 512MB RAM
- **Red**: Acceso HTTPS a la API de Segpres

### Dependencias Principales
- gRPC Java 1.52.1
- iText PDF 5.5.13.3
- Bouncy Castle Crypto 1.70
- Apache HttpClient 4.5.14
- Elastic APM 1.35.0

## 🚀 Instalación

### Desde el Código Fuente

```bash
# Clonar el repositorio
git clone <repository-url>
cd grpc-segpres-hash-signer

# Compilar el proyecto
mvn clean package

# El JAR se genera en target/grpc-segpres-hash-signer-1.0.0-jar-with-dependencies.jar
```

### Con Docker

```bash
# Construir la imagen
docker build -t grpc-segpres-signer .

# Ejecutar el contenedor
docker run -d \
  -p 8080:8080 \
  -e SEGPRES_API_TOKEN_KEY=your_api_token_key \
  -e SEGPRES_SECRET=your_secret \
  -e SEGPRES_BASE_URL=https://api.firma.cert.digital.gob.cl \
  -v /path/to/secret:/app/secret \
  grpc-segpres-signer
```

## ⚙️ Configuración

### Variables de Entorno Requeridas

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `SEGPRES_API_TOKEN_KEY` | Token de API de Segpres | `your-api-token-key-here` |
| `SEGPRES_SECRET` | Secreto para firma | `your-secret-here` |
| `SEGPRES_BASE_URL` | URL base de la API de Segpres | `https://api.firma.cert.digital.gob.cl` |

### Variables de Entorno Opcionales

| Variable | Descripción | Valor por Defecto | Rango |
|----------|-------------|-------------------|-------|
| `APP_THREADS` | Número de hilos del servidor | `5` | `1-20` |
| `APP_MAX_INBOUND_MESSAGE_SIZE` | Tamaño máximo de mensaje (en bytes) | `4194304` (4 MB) | `1048576-104857600` (1-100 MB) |
| `APP_TIMEOUT` | Timeout de conexión (en milisegundos) | `60000` (60 segundos) | `10000-300000` (10-300 segundos) |
| `APP_LOGGING_LEVEL` | Nivel de log | `INFO` | `TRACE,DEBUG,INFO,WARN,ERROR` |
| `APP_TIMEZONE` | Zona horaria | Sistema | `America/Santiago` |

### Configuración de Certificados

El servicio requiere una clave privada RSA para descifrar las contraseñas:

```bash
# Crear directorio de secretos
mkdir -p secret

# Colocar la clave privada
cp private.pem secret/private.pem
```

## 🔧 Uso

### Cliente gRPC (Ejemplo en Java)

```java
// Crear canal gRPC
ManagedChannel channel = ManagedChannelBuilder
    .forAddress("HOST", 8080)
    .usePlaintext()
    .build();

SignerGrpc.SignerBlockingStub stub = SignerGrpc.newBlockingStub(channel);

// Preparar solicitud
SignRequest request = SignRequest.newBuilder()
    .setRut("12345678-9")
    .setPassword(encryptedPassword) // Encriptado con RSA
    .setFile(ByteString.copyFrom(pdfBytes))
    .setAttended(true)
    .setPage(1)
    .setLlx(100)
    .setLly(100)
    .setUrx(200)
    .setUry(150)
    .setSignature(ByteString.copyFrom(signatureImageBytes))
    .build();

// Ejecutar firma
SignReply response = stub.send(request);

if (response.getSuccess()) {
    byte[] signedPdf = response.getFile().toByteArray();
    // Procesar PDF firmado
} else {
    System.err.println("Error: " + response.getMessage());
}
```

### Ejemplo con grpcurl

```bash
# Listar servicios disponibles
grpcurl -plaintext localhost:8080 list

# Describir el servicio
grpcurl -plaintext localhost:8080 describe signerGRPC.Signer

# Realizar una firma (ejemplo básico)
grpcurl -plaintext -d '{
  "rut": "12345678-9",
  "password": "encrypted_password",
  "file": "base64_encoded_pdf",
  "attended": true
}' localhost:8080 signerGRPC.Signer/Send
```

## 📖 API Reference

### Servicio: `signerGRPC.Signer`

#### Método: `Send`

**Request: `SignRequest`**

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `name` | `string` | Nombre del archivo |
| `file` | `bytes` | Datos del PDF a firmar |
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
| `file` | `bytes` | PDF firmado (si success=true) |
| `message` | `string` | Mensaje descriptivo del resultado |
| `invalid_password` | `bool` | Indica si la contraseña/OTP es inválida |
| `retry` | `bool` | Indica si se debe reintentar la operación |

## 📁 Estructura del Proyecto

```
grpc-segpres-hash-signer/
├── src/main/java/cl/uchile/fea/
│   ├── App.java                    # Clase principal
│   ├── Utils.java                  # Utilidades generales
│   ├── HttpStatusCode.java         # Códigos de estado HTTP
│   ├── grpc/
│   │   ├── SignerServer.java       # Servidor gRPC
│   │   ├── SignerService.java      # Implementación del servicio
│   │   └── SignException.java      # Excepción personalizada
│   ├── jwt/
│   │   └── JwtUtil.java           # Utilidades JWT
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
│   └── logback.xml                # Configuración de logging
├── signerGRPC.proto               # Definición del servicio gRPC
├── pom.xml                        # Configuración Maven
├── Dockerfile                     # Imagen Docker
└── README.md                      # Este archivo
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

# Ejecutar tests
mvn test

# Ejecutar en modo desarrollo
mvn exec:java -Dexec.mainClass="cl.uchile.fea.App"
```

### Generación de Código Proto

Si modificas `signerGRPC.proto`, regenera las clases:

```bash
# Instalar protoc
# macOS: brew install protobuf
# Ubuntu: apt-get install protobuf-compiler

# Generar clases Java (si es necesario)
protoc --java_out=src/main/java signerGRPC.proto
```

### Variables de Desarrollo

```bash
# Archivo .env para desarrollo
export SEGPRES_API_TOKEN_KEY="dev-token"
export SEGPRES_SECRET="dev-secret"
export SEGPRES_BASE_URL="https://api-dev.segpres.cl"
export APP_LOGGING_LEVEL="DEBUG"
export APP_TIMEOUT="30000"
```

## 🐳 Docker

### Construcción de la Imagen

```bash
# Construcción estándar
docker build -t grpc-segpres-signer:latest .

# Construcción con argumentos
docker build \
  --build-arg MAVEN_OPTS="-Xmx1024m" \
  -t grpc-segpres-signer:latest .
```

### Ejecución con Docker Compose

```yaml
# docker-compose.yml
version: '3.8'
services:
  grpc-signer:
    image: grpc-segpres-signer:latest
    ports:
      - "8080:8080"
    environment:
      - SEGPRES_API_TOKEN_KEY=${SEGPRES_API_TOKEN_KEY}
      - SEGPRES_SECRET=${SEGPRES_SECRET}
      - SEGPRES_BASE_URL=${SEGPRES_BASE_URL}
      - APP_THREADS=10
      - APP_LOGGING_LEVEL=INFO
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
  name: grpc-segpres-signer
spec:
  replicas: 3
  selector:
    matchLabels:
      app: grpc-segpres-signer
  template:
    metadata:
      labels:
        app: grpc-segpres-signer
    spec:
      containers:
      - name: grpc-signer
        image: grpc-segpres-signer:latest
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
          value: "https://api.segpres.cl"
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
# Variables de entorno para APM
export ELASTIC_APM_SERVICE_NAME="grpc-segpres-signer"
export ELASTIC_APM_SERVER_URLS="https://apm.example.com:8200"
export ELASTIC_APM_SECRET_TOKEN="your-apm-token"
export ELASTIC_APM_ENVIRONMENT="production"
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

Este proyecto está bajo la [licencia AGPLv3](https://itextpdf.com/how-buy/AGPLv3-license) heredado del proyecto [itext](https://github.com/itext/itextpdf).

## 📞 Soporte

Para soporte técnico:

- **Email**: arquitectura-vti@uchile.cl
- **Documentación**: Consulta la documentación interna de FEA
- **Issues**: Usa el sistema de issues del repositorio

---

**Desarrollado por**: VTI - Universidad de Chile  
**Mantenido por**: Equipo FEA  
**Versión**: 1.0.0
