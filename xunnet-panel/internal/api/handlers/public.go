package handlers

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/Hinderchik/XunnetClient/xunnet-panel/internal/db"
)

func Status(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{
		"status":  "ok",
		"version": "2.0.0",
	})
}

func FederationInfo(c *gin.Context) {
	var count int64
	db.DB.Model(&db.Server{}).Count(&count)
	c.JSON(http.StatusOK, gin.H{
		"panel_id":      "panel_default",
		"name":          "Xunnet Panel",
		"version":       "2.0.0",
		"role":          "master",
		"servers_count": count,
		"capabilities":  []string{"vless", "vmess", "trojan", "wireguard"},
	})
}

func GetSubscription(c *gin.Context) {
	id := c.Param("id")
	c.JSON(http.StatusOK, gin.H{
		"id":      id,
		"format":  "xunnet",
		"servers": []gin.H{},
	})
}
