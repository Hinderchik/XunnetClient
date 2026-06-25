package handlers

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/Hinderchik/XunnetClient/xunnet-panel/internal/db"
)

func ListSubscriptions(c *gin.Context) {
	var subs []db.Subscription
	db.DB.Find(&subs)
	c.JSON(http.StatusOK, subs)
}

func CreateSubscription(c *gin.Context) {
	var sub db.Subscription
	if err := c.ShouldBindJSON(&sub); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	db.DB.Create(&sub)
	c.JSON(http.StatusCreated, sub)
}

func GetSubscriptionByID(c *gin.Context) {
	var sub db.Subscription
	if err := db.DB.First(&sub, "id = ?", c.Param("id")).Error; err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "not found"})
		return
	}
	c.JSON(http.StatusOK, sub)
}

func UpdateSubscription(c *gin.Context) {
	var sub db.Subscription
	if err := db.DB.First(&sub, "id = ?", c.Param("id")).Error; err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "not found"})
		return
	}
	if err := c.ShouldBindJSON(&sub); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	db.DB.Save(&sub)
	c.JSON(http.StatusOK, sub)
}

func DeleteSubscription(c *gin.Context) {
	db.DB.Delete(&db.Subscription{}, "id = ?", c.Param("id"))
	c.Status(http.StatusNoContent)
}
