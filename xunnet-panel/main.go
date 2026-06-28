package main

import (
	"embed"
	"io/fs"
	"log"
	"net/http"
	"os"

	"github.com/gin-gonic/gin"
	"github.com/Hinderchik/XunnetClient/xunnet-panel/internal/api/handlers"
	"github.com/Hinderchik/XunnetClient/xunnet-panel/internal/api/middlewares"
	"github.com/Hinderchik/XunnetClient/xunnet-panel/internal/api/routes"
	"github.com/Hinderchik/XunnetClient/xunnet-panel/internal/config"
	"github.com/Hinderchik/XunnetClient/xunnet-panel/internal/db"
)

//go:embed all:web/dist
var webDist embed.FS

// version is set at build time via -ldflags "-X main.version=<tag>".
var version = "dev"

func main() {
	log.Printf("Xunnet Panel %s starting...", version)
	cfg := config.Load()
	if err := db.Init(cfg.DatabaseDriver, cfg.DatabaseDSN); err != nil {
		log.Fatalf("failed to init database: %v", err)
	}
	if err := db.Migrate(); err != nil {
		log.Fatalf("failed to migrate database: %v", err)
	}

	r := gin.Default()
	r.Use(middlewares.CORS())

	api := r.Group("/api")
	{
		public := api.Group("/public")
		{
			public.GET("/status", handlers.Status)
			public.GET("/federation/info", handlers.FederationInfo)
			public.GET("/subscription/:id", handlers.GetSubscription)
		}

		v1 := api.Group("/v1")
		v1.Use(middlewares.JWTAuth(cfg.SecretKey))
		{
			routes.RegisterUserRoutes(v1)
			routes.RegisterServerRoutes(v1)
			routes.RegisterSubscriptionRoutes(v1)
			routes.RegisterFederationRoutes(v1)
			routes.RegisterStatsRoutes(v1)
		}
	}

	// Serve embedded React frontend
	staticFS, err := fs.Sub(webDist, ".")
	if err == nil {
		r.StaticFS("/", http.FS(staticFS))
		r.NoRoute(func(c *gin.Context) {
			c.FileFromFS("index.html", http.FS(staticFS))
		})
	} else {
		log.Printf("warning: web/dist not embedded: %v", err)
	}

	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}
	log.Printf("Xunnet Panel listening on :%s", port)
	if err := r.Run(":" + port); err != nil {
		log.Fatalf("server error: %v", err)
	}
}
