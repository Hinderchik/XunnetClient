package routes

import (
	"github.com/gin-gonic/gin"
	"github.com/Hinderchik/XunnetClient/xunnet-panel/internal/api/handlers"
)

func RegisterUserRoutes(r *gin.RouterGroup) {
	g := r.Group("/users")
	{
		g.GET("", handlers.ListUsers)
		g.POST("", handlers.CreateUser)
		g.GET("/:id", handlers.GetUser)
		g.PUT("/:id", handlers.UpdateUser)
		g.DELETE("/:id", handlers.DeleteUser)
	}
}

func RegisterServerRoutes(r *gin.RouterGroup) {
	g := r.Group("/servers")
	{
		g.GET("", handlers.ListServers)
		g.POST("", handlers.CreateServer)
		g.GET("/:id", handlers.GetServer)
		g.PUT("/:id", handlers.UpdateServer)
		g.DELETE("/:id", handlers.DeleteServer)
	}
}

func RegisterSubscriptionRoutes(r *gin.RouterGroup) {
	g := r.Group("/subscriptions")
	{
		g.GET("", handlers.ListSubscriptions)
		g.POST("", handlers.CreateSubscription)
		g.GET("/:id", handlers.GetSubscriptionByID)
		g.PUT("/:id", handlers.UpdateSubscription)
		g.DELETE("/:id", handlers.DeleteSubscription)
	}
}

func RegisterFederationRoutes(r *gin.RouterGroup) {
	g := r.Group("/federation")
	{
		g.GET("/panels", handlers.ListPanels)
		g.POST("/panels", handlers.CreatePanel)
		g.GET("/panels/:id", handlers.GetPanel)
		g.PUT("/panels/:id", handlers.UpdatePanel)
		g.DELETE("/panels/:id", handlers.DeletePanel)
		g.POST("/panels/:id/sync", handlers.SyncPanel)
		g.POST("/sync-all", handlers.SyncAllPanels)
	}
}

func RegisterStatsRoutes(r *gin.RouterGroup) {
	g := r.Group("/stats")
	{
		g.GET("/overview", handlers.StatsOverview)
		g.GET("/traffic", handlers.StatsTraffic)
		g.GET("/online", handlers.StatsOnline)
		g.GET("/logs", handlers.StatsLogs)
	}
}
