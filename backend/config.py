from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    neo4j_uri: str
    neo4j_user: str
    neo4j_password: str
    neo4j_database: str = "neo4j"
    jwt_secret: str
    jwt_algorithm: str = "HS256"
    jwt_expire_minutes: int = 60

    # Comma-separated list of allowed browser origins for CORS.
    # In production, override with your deployed Vercel domain.
    cors_origins: str = (
        "http://localhost:8913,"
        "http://localhost:4173,"
        "http://127.0.0.1:8913,"
        "http://127.0.0.1:4173,"
        "https://mirage-bank.vercel.app"
    )

    # Email — optional. Mailgun is preferred, then Resend, then SMTP.
    # If none configured, emails are printed to console (dev mode).
    mailgun_domain: str = ""
    mailgun_api_key: str = ""
    resend_api_key: str = ""
    smtp_host: str = ""
    smtp_port: int = 587
    smtp_user: str = ""
    smtp_password: str = ""
    smtp_from: str = "noreply@miragebank.com"

    app_env: str = "development"

    model_config = {
        "env_file": "../.env",
        "extra": "ignore",
    }


settings = Settings()