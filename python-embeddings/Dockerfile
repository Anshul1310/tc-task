

FROM python:3.11-slim

WORKDIR /app

# Silence pip root warning inside Docker
ENV PIP_ROOT_USER_ACTION=ignore

# Install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy server code
COPY server.py .

# Expose port
EXPOSE 5050

# Start the FastAPI server
CMD ["uvicorn", "server:app", "--host", "0.0.0.0", "--port", "5050"]
