# Docker 실행

## 기본 구성

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`
- MySQL: Aiven 공용 `in_short` 데이터베이스
- Redis: Redis Cloud 공용 데이터베이스

`.env.example`을 `.env`로 복사한 뒤 팀에서 공유받은 MySQL 및 Redis 비밀번호를 입력한다. `.env`는 Git에 커밋하지 않는다.

```bash
docker compose up -d --build
docker compose ps
```

공용 MySQL은 SSL 연결이 필수이므로 `MYSQL_SSL_MODE=REQUIRED`를 사용한다. 무료 서비스는 장기간 사용하지 않으면 자동으로 중지될 수 있으며, Aiven Console에서 다시 시작할 수 있다.

## 선택 사항: 로컬 MySQL

공용 MySQL을 사용할 수 없을 때만 로컬 MySQL 프로필을 실행한다. 이때 `.env`의 MySQL 호스트, 포트, 사용자, 비밀번호와 SSL 모드를 로컬 값으로 변경해야 한다.

```bash
docker compose --profile local-mysql up -d mysql
```

로컬 MySQL의 권장 설정은 `MYSQL_HOST=mysql`, `MYSQL_PORT=3306`, `MYSQL_SSL_MODE=PREFERRED`이다.

## 선택 사항: 로컬 Redis

```bash
docker compose --profile local-redis up -d redis
docker compose exec redis redis-cli ping
```

## 종료

```bash
docker compose down
```

`docker compose down -v`는 로컬 MySQL 볼륨의 데이터까지 삭제하므로 필요한 경우에만 실행한다. 공용 Aiven MySQL 데이터는 이 명령의 영향을 받지 않는다.
