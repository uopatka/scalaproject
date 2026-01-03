# Projekt Scala

Julia Masiarz

Natalia Uścińska - frontend

Paweł Witkowski

Aleksandra Woźnica - scala, backend, frontend

## Ważne
Defaultowy branch (main) nazywa się play-framework, zapewne zmienimy w przyszłości nazwę na main.
Na ten moment pobierając kod z Githuba używajcie ```git pull origin play-framework``` 

## Użyte frameworki
- **Play** https://www.playframework.com/
- **Bootstrap 5** - do frontendu. Dokumentacja: https://getbootstrap.com/docs/5.0/getting-started/introduction/

## Instalacja
-  **sbt (ver. sbt-1.11.7)** należy pobrać ze strony: www.scala-sbt.org/download/

## Jak uruchomić
1) Otwórz terminal, przejdź w nim do folderu z projektem, np. ```cd scalaproject```
2) Uruchom aplikację wpisując ```sbt run```
3) Otwórz przeglądarkę i wejdź na http://localhost:9000/

## DB 
Aby testować z bazą danych, musicie przygotować bazę danych lokalnie (`your_username` **musi być waszym userem z linuxa**). Musicie też mieć pobranego postgresql:

```
sudo -u postgres createuser -P your_username
sudo -u postgres createdb -O your_username bugshelv
```

```
export DB_URL="jdbc:postgresql://localhost:5432/bugshelv"
export DB_USER="your_username"
export DB_PASSWORD="your_password"
```

```
psql "$DB_URL" -U "$DB_USER" -f create_tables.sql 
```

## Ikonki (do wybrania)
https://www.svgrepo.com/collection/iconship-interface-icons

https://www.svgrepo.com/collection/software-mansion-curved-line-icons
