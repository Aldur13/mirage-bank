FROM python:3.13-slim

WORKDIR /app

# Copy backend code and requirements
COPY backend/ ./backend/
COPY frontend/ ./frontend/
COPY requirements.txt .

# Install dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Expose port
EXPOSE 8000

# Start the app from the backend directory
CMD ["uvicorn", "backend.main:app", "--host", "0.0.0.0", "--port", "8000"]
