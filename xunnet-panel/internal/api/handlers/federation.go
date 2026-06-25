package handlers

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/Hinderchik/XunnetClient/xunnet-panel/internal/db"
)

func ListPanels(c *gin.Context) {
	var panels []db.FederatedPanel
	db.DB.Find(&panels)
	c.JSON(http.StatusOK, panels)
}

func CreatePanel(c *gin.Context) {
	var panel db.FederatedPanel
	if err := c.ShouldBindJSON(&panel); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	db.DB.Create(&panel)
	c.JSON(http.StatusCreated, panel)
}

func GetPanel(c *gin.Context) {
	var panel db.FederatedPanel
	if err := db.DB.First(&panel, "id = ?", c.Param("id")).Error; err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "not found"})
		return
	}
	c.JSON(http.StatusOK, panel)
}

func UpdatePanel(c *gin.Context) {
	var panel db.FederatedPanel
	if err := db.DB.First(&panel, "id = ?", c.Param("id")).Error; err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "not found"})
		return
	}
	if err := c.ShouldBindJSON(&panel); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	db.DB.Save(&panel)
	c.JSON(http.StatusOK, panel)
}

func DeletePanel(c *gin.Context) {
	db.DB.Delete(&db.FederatedPanel{}, "id = ?", c.Param("id"))
	c.Status(http.StatusNoContent)
}

func SyncPanel(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{"status": "sync started", "panel_id": c.Param("id")})
}

func SyncAllPanels(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{"status": "sync all started"})
}
