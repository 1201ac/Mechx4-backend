package com.nerlogix.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "nerlogix")
public class NerlogixProperties {

    private Jwt jwt = new Jwt();
    private Weather weather = new Weather();
    private Simulation simulation = new Simulation();
    private Uploads uploads = new Uploads();
    private Cors cors = new Cors();
    private Google google = new Google();

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Weather getWeather() {
        return weather;
    }

    public void setWeather(Weather weather) {
        this.weather = weather;
    }

    public Simulation getSimulation() {
        return simulation;
    }

    public void setSimulation(Simulation simulation) {
        this.simulation = simulation;
    }

    public Uploads getUploads() {
        return uploads;
    }

    public void setUploads(Uploads uploads) {
        this.uploads = uploads;
    }

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public Google getGoogle() {
        return google;
    }

    public void setGoogle(Google google) {
        this.google = google;
    }

    public static class Jwt {
        private String secret = "dev_secret_key_must_be_at_least_32_bytes_long_for_security_reasons_12345";
        private long accessTokenExpirationMs = 900000;
        private long refreshTokenExpirationMs = 604800000;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getAccessTokenExpirationMs() {
            return accessTokenExpirationMs;
        }

        public void setAccessTokenExpirationMs(long accessTokenExpirationMs) {
            this.accessTokenExpirationMs = accessTokenExpirationMs;
        }

        public long getRefreshTokenExpirationMs() {
            return refreshTokenExpirationMs;
        }

        public void setRefreshTokenExpirationMs(long refreshTokenExpirationMs) {
            this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        }
    }

    public static class Weather {
        private boolean autoFetch = true;

        public boolean isAutoFetch() {
            return autoFetch;
        }

        public void setAutoFetch(boolean autoFetch) {
            this.autoFetch = autoFetch;
        }
    }

    public static class Simulation {
        private boolean enabled = true;
        private long tickMs = 3000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getTickMs() {
            return tickMs;
        }

        public void setTickMs(long tickMs) {
            this.tickMs = tickMs;
        }
    }

    public static class Uploads {
        private String dir = "./uploads";

        public String getDir() {
            return dir;
        }

        public void setDir(String dir) {
            this.dir = dir;
        }
    }

    public static class Cors {
        private String allowedOrigins = "http://localhost:5173,http://localhost:8080";

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Google {
        private Routes routes = new Routes();

        public Routes getRoutes() {
            return routes;
        }

        public void setRoutes(Routes routes) {
            this.routes = routes;
        }

        public static class Routes {
            private boolean enabled = true;
            private int timeoutMs = 3000;
            private int cacheTtlSeconds = 300;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public int getTimeoutMs() {
                return timeoutMs;
            }

            public void setTimeoutMs(int timeoutMs) {
                this.timeoutMs = timeoutMs;
            }

            public int getCacheTtlSeconds() {
                return cacheTtlSeconds;
            }

            public void setCacheTtlSeconds(int cacheTtlSeconds) {
                this.cacheTtlSeconds = cacheTtlSeconds;
            }
        }
    }
}
