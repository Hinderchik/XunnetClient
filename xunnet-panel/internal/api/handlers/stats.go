package handlers

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

func StatsOverview(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{
		"users":   0,
		"servers": 0,
		"traffic": gin.H{"upload": 0, "download": 0},
	})
}

func StatsTraffic(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{"traffic": []gin.H{}})
}

func StatsOnline(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{"online": 0})
}

func StatsLogs(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{"logs": []string{}})
}
