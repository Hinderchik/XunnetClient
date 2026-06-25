package db

import (
	"gorm.io/driver/sqlite"
	"gorm.io/gorm"
)

var DB *gorm.DB

func Init(driver, dsn string) error {
	var err error
	if driver == "sqlite" {
		DB, err = gorm.Open(sqlite.Open(dsn), &gorm.Config{})
	}
	return err
}

func Migrate() error {
	return DB.AutoMigrate(
		&User{},
		&Server{},
		&Subscription{},
		&FederatedPanel{},
	)
}
