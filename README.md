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

## Roadmap

### Scala

- [ ] Dodaj książkę
  - [ ] Dodaj książkę bez ISBN
- [ ] Dodać filtrowanie książek

### Frontend

- [ ] Wyświetlić wszystkie pola z Book Entry i Book w detalach
  - [ ] match case BookStatus -> na stronie ma być "w trakcie", "przeczytana" itp.
  - [ ] match case liczba stron -> na stronie ma być coś typu '-' lub 'brak informacji' gdy pobrane jest 0
- [ ] Dodać filtrowanie książek
- [ ] Form errors (ujednolicić, sprawić żeby się nie powtarzały itp.)
  - [ ] logowanie
  - [ ] dodawanie książki (sam ISBN)
  - [ ] tworzenie książki

### API

- [ ] Dodać wyszukiwanie za pomocą tytułu przez API?

### Testowanie
- [ ] Analiza domeny
  - [ ] Przegląd modeli oraz routingu
  - [ ] Zrozumienie workflow: dodanie książki -> zapis w database -> wyśwetlenie w UI
- [ ] Testy Jednostkowe
  - [ ] Walidacja modeli domenowych i logiki pomocniczej
  - [ ] Sprawdzenie reguł: komunikaty, obsługa statusów BookStatus i liczby storn.


## Jak uruchomić
1) Otwórz terminal, przejdź w nim do folderu z projektem, np. ```cd scalaproject```
2) Uruchom aplikację wpisując ```sbt run```
3) Otwórz przeglądarkę i wejdź na http://localhost:9000/

## DB 
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

## Ikonki
Na razie używamy https://www.svgrepo.com/collection/iconship-interface-icons

Inne ładne https://www.svgrepo.com/collection/software-mansion-curved-line-icons


## Roadmap Archive

### Scala

- [x] Wyświetl książki
  - [x] Wyświetlaj BookEntries
  - [x] BookEntries wg. usera
  - [x] "Już masz tą książkę"
  - [x] "Ta książka nie istnieje"
- [x] Dodaj książkę
  - [x] Stwórz książkę jak isbn nie jest w OpenLibrary
- [x] Pokaż detale w prawym panelu
- [x] Na gościu nie widać książek
- [x] Dodać edytowanie BookEntry
- [x] Dodać usuwanie BookEntry
- [x] Dodać zapisywanie dla guesta?
- [x] WAŻNE - postarać się usunąć warningi przy sbt clean compile (osobny commit proszę)

### UI

- [x] Dodawanie książki
- [ ] Wyświetlić wszystkie pola z Book Entry i Book w detalach
  - [x] pagesRead (BookEntry)
  - [x] cover (Book)
- [x] Dodać edytowanie BookEntry w prawym panelu
- [x] Dodać usuwanie BookEntry
- [x] Dodać okładki książek
- [x] Dodać logowanie!!!!!!!

### Backend

- [x] DB
- [x] Przerzucić z repositories do DB

### API

- [x] Wybrać API
- [x] Dodać dodawanie przez API