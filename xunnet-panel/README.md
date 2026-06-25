# Xunnet Panel

Web-based server management panel for Xunnet ecosystem. Built with Go (Gin) and React.

## Features

- User and server management
- Multi-subscription and aggregation
- Federation between panels
- RESTful API with JWT auth
- SQLite/PostgreSQL support

## Run

```bash
cd xunnet-panel
go run ./cmd/server
```

## Docker

```bash
docker-compose up --build
```

## API

- Public: `/api/public/...`
- Private: `/api/v1/...` (JWT required)

## License

GPL-3.0
