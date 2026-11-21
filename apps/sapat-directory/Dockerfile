FROM python:3.12-slim
RUN pip install --no-cache-dir pycryptodome==3.21.0 && useradd -r -u 10001 dir && mkdir /data && chown dir /data
COPY directory.py /app/directory.py
USER dir
VOLUME /data
EXPOSE 8000 8080
CMD ["python3", "/app/directory.py"]
