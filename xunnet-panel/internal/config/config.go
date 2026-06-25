package config

import "os"

type Config struct {
	SecretKey     string
	DatabaseDriver string
	DatabaseDSN   string
}

func Load() Config {
	return Config{
		SecretKey:      getEnv("SECRET_KEY", "change-me"),
		DatabaseDriver: getEnv("DB_DRIVER", "sqlite"),
		DatabaseDSN:    getEnv("DB_DSN", "xunnet.db"),
	}
}

func getEnv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
