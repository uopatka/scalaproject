<div style="text-align:right; font-weight:bold;"> 
    Projekt końcowy na <em>Programowanie w języku Scala</em>
</div>

<h1 style="text-align:center; font-size:3em; margin-bottom:0.2em;"> 
    <a href="https://bugshelv.aiwo.pl" style="color:OliveDrab; text-decoration:none;">
        🕮 Bugshelv
    </a>
</h1>

### Zespół:

- **Julia Masiarz** - Scala, frontend  
- **Natalia Uścińska** - frontend, UI, Docker  
- **Paweł Witkowski**  
- **Aleksandra Woźnica** - Scala, backend, frontend


## Użyte frameworki
- **Play** https://www.playframework.com/
- **Bootstrap 5** - do frontendu. Dokumentacja: https://getbootstrap.com/docs/5.0/getting-started/introduction/

## Wymagania
-  **sbt (ver. sbt-1.11.7)** instalacja: www.scala-sbt.org/download/
- **postgreSQL** używając Dockera nie jest wymagane pobieranie na swój OS, w przeciwnym wypadku jest wymagane.
- **Docker** instalacja: https://docs.docker.com/engine/install

## Jak uruchomić
1) Skopiuj .env.sample, zapisz jako .env i uzupełnij swoimi zmiennymi
2) Otwórz terminal, przejdź w nim do folderu z projektem, np. ```cd scalaproject```
3) Uruchom bazę danych ```docker compose up -d db```. 
#### Inicjalizacja tabel (tylko za pierwszym razem) 
Wersja na Linuxa: 
```
docker exec -i bugshelv_db psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} < create_tables.sql
``` 
Wersja na Windowsa: 
```
Get-Content create_tables.sql | docker exec -i bugshelv_db psql -U NAZWA_UŻYTKOWNIKA_Z_ENV -d bugshelv
```
4) Uruchom aplikację wpisując ```sbt run```
5) Otwórz przeglądarkę i wejdź na http://localhost:9000/

## DB - bez Dockera
Aby testować z bazą danych, musicie przygotować bazę danych lokalnie (`your_username` **musi być waszym userem z linuxa**):

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
