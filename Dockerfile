FROM criyle/go-judge:v1.9.9 AS go-judge 
FROM debian:latest
# 安装需要的编译器
RUN apt update && apt install -y g++
WORKDIR /opt
COPY --from=go-judge /opt/go-judge /opt/mount.yaml /opt/
EXPOSE 5050/tcp 5051/tcp 5052/tcp
ENTRYPOINT ["./go-judge"]