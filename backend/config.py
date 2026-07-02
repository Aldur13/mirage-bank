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

    app_env: str = "development"

    model_config = {
        "env_file": "../.env",
        "extra": "ignore",
    }


settings = Settings()