FROM golang:1.22-alpine AS build
WORKDIR /app
COPY . .
RUN go mod tidy && CGO_ENABLED=0 GOOS=linux go build -trimpath -ldflags="-s -w" -o mqtt-bridge .

FROM alpine:3.19
RUN addgroup -S bridge && adduser -S bridge -G bridge
USER bridge
COPY --from=build /app/mqtt-bridge /usr/local/bin/mqtt-bridge
EXPOSE 8090
ENTRYPOINT ["mqtt-bridge"]
