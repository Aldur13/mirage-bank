FROM python:3.13-slim

# Copy everything to /app
COPY . /app/

WORKDIR /app/backend

# Install dependencies from root
RUN pip install --no-cache-dir -r /app/requirements.txt

# Expose port
EXPOSE 8000

# Start the app from backend directory (frontend files are at ../frontend)
CMD uvicorn main:app --host 0.0.0.0 --port ${PORT:-8000}
