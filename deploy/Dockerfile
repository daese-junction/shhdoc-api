# jar 는 GitHub 러너에서 빌드해 deploy/app.jar 로 올려둔다 (.github/workflows/deploy.yml).
# 서버는 2 vCPU 라 여기서 빌드하면 3 분이 걸리고, 빌드 캐시가 디스크를 2GB 씩 채운다.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY deploy/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
