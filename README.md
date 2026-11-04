# 🛵 Administrapochi

Sistema web para la administración de vehículos, conductores, cuotas, pagos y operaciones relacionadas.

Administrapochi nace como un proyecto personal para centralizar y simplificar la administración de una pequeña flotilla, sustituyendo registros manuales por una aplicación web sencilla y accesible.

## 🏗️ Arquitectura

El proyecto está dividido en frontend, backend y base de datos.

```text
                    ┌──────────────┐
                    │   Frontend   │
                    │    Web UI    │
                    └──────┬───────┘
                           │
                         REST
                           │
                    ┌──────▼───────┐
                    │   Backend    │
                    │ Spring Boot  │
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
                    │ PostgreSQL   │
                    └──────────────┘
```

Los servicios de la aplicación serán ejecutados mediante Docker.

## 🛠️ Tecnologías

### Backend

- Java 25 (OpenJDK)
- Spring Boot 4
- Spring Web
- Spring Data JPA
- Hibernate
- Bean Validation
- Maven

### Base de datos

- PostgreSQL 17

### Infraestructura

- Docker
- Docker Compose
- Raspberry Pi
- Raspberry Pi OS / Debian
- Git
- GitHub

### Frontend

Pendiente por definir.

## 📁 Estructura

```text
administrapochi/
├── backend/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
│
├── frontend/
│
├── docker-compose.yml
├── .env
├── .gitignore
└── README.md
```

> El archivo `.env` contiene configuración local y secretos, por lo que no debe almacenarse en Git.

## 🚀 Ejecución

La aplicación completa será ejecutada utilizando Docker Compose.

```bash
docker compose up -d --build
```

Para revisar los servicios:

```bash
docker compose ps
```

Para detenerlos:

```bash
docker compose down
```

## 💾 Persistencia

PostgreSQL utiliza un volumen de Docker para mantener los datos aunque el contenedor sea eliminado o recreado.

```text
postgres_data
```

Eliminar el volumen eliminará también los datos almacenados:

```bash
docker compose down -v
```

> ⚠️ No utilizar `-v` en un entorno con información que deba conservarse.

## 🧪 Estado del proyecto

Actualmente en desarrollo.

### Roadmap inicial

- [x] Preparar Raspberry Pi
- [x] Instalar y configurar Docker
- [x] Configurar PostgreSQL
- [x] Configurar persistencia de PostgreSQL
- [x] Crear repositorio Git
- [x] Configurar acceso SSH con GitHub
- [x] Crear proyecto Spring Boot
- [ ] Dockerizar backend
- [ ] Conectar Spring Boot con PostgreSQL
- [ ] Implementar módulo de vehículos
- [ ] Implementar módulo de conductores
- [ ] Implementar control de cuotas y pagos
- [ ] Definir e implementar frontend
- [ ] Implementar autenticación y seguridad
- [ ] Automatizar despliegue en Raspberry Pi

## 🔄 Flujo de desarrollo

El desarrollo se realiza en una computadora y la Raspberry Pi funciona como servidor.

```text
Development PC
      │
      │ git push
      ▼
    GitHub
      │
      │ git pull
      ▼
 Raspberry Pi
      │
      ▼
Docker Compose
      │
      ├── PostgreSQL
      ├── Backend
      └── Frontend
```

## 🔐 Seguridad

Los secretos y contraseñas no deben almacenarse en el repositorio.

Actualmente las variables sensibles se mantienen en:

```text
.env
```

El archivo está excluido mediante `.gitignore`.

## 📜 Licencia

Proyecto privado de uso personal.
