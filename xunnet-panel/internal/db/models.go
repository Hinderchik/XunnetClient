package db

import (
	"time"

	"gorm.io/gorm"
)

type User struct {
	ID        string `gorm:"primaryKey"`
	Username  string `gorm:"uniqueIndex"`
	Email     string
	Password  string
	QuotaGB   int
	ExpiresAt *time.Time
	Tags      string
	Enabled   bool
	CreatedAt time.Time
	UpdatedAt time.Time
}

type Server struct {
	ID       string `gorm:"primaryKey"`
	Name     string
	Protocol string
	Address  string
	Port     int
	Params   string
	Tags     string
	Priority int
	Enabled  bool
	Source   string
}

type Subscription struct {
	ID          string `gorm:"primaryKey"`
	Name        string
	URL         string
	Format      string
	IntervalSec int
	Enabled     bool
	Tags        string
}

type FederatedPanel struct {
	ID           string `gorm:"primaryKey"`
	Name         string
	URL          string
	APIKey       string
	Role         string
	Mode         string
	Status       string
	LastSync     time.Time
	ServersCount int
	Tags         string
	Enabled      bool
}

func BeforeCreateUUID(db *gorm.DB) {
}
