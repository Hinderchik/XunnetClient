package handlers

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/Hinderchik/XunnetClient/xunnet-panel/internal/db"
)

func ListServers(c *gin.Context) {
	var servers []db.Server
	db.DB.Find(&servers)
	c.JSON(http.StatusOK, servers)
}

func CreateServer(c *gin.Context) {
	var server db.Server
	if err := c.ShouldBindJSON(&server); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	db.DB.Create(&server)
	c.JSON(http.StatusCreated, server)
}

func GetServer(c *gin.Context) {
	var server db.Server
	if err := db.DB.First(&server, "id = ?", c.Param("id")).Error; err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "not found"})
		return
	}
	c.JSON(http.StatusOK, server)
}

func UpdateServer(c *gin.Context) {
	var server db.Server
	if err := db.DB.First(&server, "id = ?", c.Param("id")).Error; err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "not found"})
		return
	}
	if err := c.ShouldBindJSON(&server); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	db.DB.Save(&server)
	c.JSON(http.StatusOK, server)
}

func DeleteServer(c *gin.Context) {
	db.DB.Delete(&db.Server{}, "id = ?", c.Param("id"))
	c.Status(http.StatusNoContent)
}
