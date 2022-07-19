# scalardb-resrevation

## Requirement

### Docker

- For Mac: Install Docker Desktop
- For Linux:

```
sudo apt-get update
sudo apt-get install docker
sudo apt-get install docker-compose
```

### JDK

- Install JDK

## Git Clone

```
git clone git@github.com:cirusthenter/scalardb-reservation.git
cd scalardb-reservation
```

## Docker Compose

```
sudo docker-compose up -d
```

- If you have this kind of error, look what is using the port `9042` by `sudo lsof -i -P -n | grep 9042`.

```
❯ sudo docker-compose up -d
Creating network "reservation-network" with the default driver
Creating cassandra-reservation ...
Creating cassandra-reservation ... error
WARNING: Host is already in use by another container

ERROR: for cassandra-reservation  Cannot start service cassandra: driver failed programming external connectivity on endpoint cassandra-reservation (de667ce95f32cd7bc0b1a56adcd1552808666d162bf80019897d3a118231b
Creating mysql-reservation     ... done

ERROR: for cassandra  Cannot start service cassandra: driver failed programming external connectivity on endpoint cassandra-reservation (de667ce95f32cd7bc0b1a56adcd1552808666d162bf80019897d3a118231ba5e): Error starting userland proxy: listen tcp4 0.0.0.0:9042: bind: address already in use
ERROR: Encountered errors while bringing up the project.
```

## Load Initial Data

初期データの読み込み

```
./gradlew run --args="LoadInitialData"
```

- 以下のようなエラーが起きたケース
    - `Caused by: java.sql.SQLException: Access denied for user 'root'@'localhost' (using password: YES)`
    - ローカル環境で MySQL が立ち上がっているのが原因の可能性大
    - `brew services stop mysql` する

### MySQL

#### customer.customers table

|customer_id|name|
|:--:|:--:|
|1|Elon Musk|
|2|Tony Stark|
|3|Steve Jobs|

#### seat.seats table

|seat_id|name|reserved_by|
|:--:|:--:|:--:|
|1|"S1"|-1|
|2|"S2"|-1|
|3|"A1"|-1|
|4|"A2"|-1|
|5|"A3"|-1|
|6|"B1"|-1|
|7|"B2"|-1|
|8|"B3"|-1|
|9|"B4"|-1|
|10|"C1"|-1|
|11|"C2"|-1|
|12|"C3"|-1|
|13|"C4"|-1|
|14|"C5"|-1|

### Cassandra

#### history.histories teble

|history_id|customer_id|operation|seat_id|timestamp|
|:--:|:--:|:--:|:--:|:--:|

## Reserve

ユーザー2が席 3, 4, 5 を予約

```
./gradlew run --args="Reserve 2 3,4,5"
```

#### MySQL seat.seats

|seat_id|name|reserved_by|
|:--:|:--:|:--:|
|1|"S1"|-1|
|2|"S2"|-1|
|3|"A1"|2|
|4|"A2"|2|
|5|"A3"|2|
|6|"B1"|-1|
|7|"B2"|-1|
|8|"B3"|-1|
|9|"B4"|-1|
|10|"C1"|-1|
|11|"C2"|-1|
|12|"C3"|-1|
|13|"C4"|-1|
|14|"C5"|-1|

#### Cassandra history.histories

|history_id|customer_id|operation|seat_id|timestamp|
|:--:|:--:|:--:|:--:|:--:|
|e3b920be-2ee3-4246-9bc0-d5cb1ff71f0d|2|reserve|3|1656652667347|
|10ea7ad3-4be1-4271-9914-098635109edc|2|reserve|4|1656652667352|
|c76735e7-1d52-498b-8b07-e73d27143ca6|2|reserve|5|1656652667356|

## Cancel Reservation

ユーザー2が席3, 5 の予約をキャンセル

```
./gradlew run --args="Cancel 2 3,5"
```


#### MySQL seat.seats

|seat_id|name|reserved_by|
|:--:|:--:|:--:|
|1|"S1"|-1|
|2|"S2"|-1|
|3|"A1"|-1|
|4|"A2"|2|
|5|"A3"|-1|
|6|"B1"|-1|
|7|"B2"|-1|
|8|"B3"|-1|
|9|"B4"|-1|
|10|"C1"|-1|
|11|"C2"|-1|
|12|"C3"|-1|
|13|"C4"|-1|
|14|"C5"|-1|

#### Cassandra history.histories

|history_id|customer_id|operation|seat_id|timestamp|
|:--:|:--:|:--:|:--:|:--:|
|e3b920be-2ee3-4246-9bc0-d5cb1ff71f0d|2|reserve|3|1656652667347|
|10ea7ad3-4be1-4271-9914-098635109edc|2|reserve|4|1656652667352|
|c76735e7-1d52-498b-8b07-e73d27143ca6|2|reserve|5|1656652667356|
|e8385020-c8f2-474b-98dd-d00328fa718f|2|cancel|3|1656652800722|
|f29f386d-7c96-476c-97a5-ecde23f3b8fe|2|cancel|5|1656652800726|

## History

ユーザー2の予約/予約削除の履歴を一覧表示

```
./gradlew run --args="GetHistories 2"
```

以下のような表示が出る

```
operation: reserve, seat: A1, time: 2022-07-01 14:17:47.347
operation: reserve, seat: A2, time: 2022-07-01 14:17:47.352
operation: reserve, seat: A3, time: 2022-07-01 14:17:47.356
operation: cancel, seat: A1, time: 2022-07-01 14:20:00.722
operation: cancel, seat: A3, time: 2022-07-01 14:20:00.726
```

## ユーザー情報表示

ユーザー2の情報を表示

```
./gradlew run --args="GetCustomerInfo 2"
```

以下のような表示が出る

```
"id": 2, "name": "Steve Jobs"
```
