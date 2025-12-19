# Projekt Scala

Julia Masiarz

Natalia Uścińska - frontend

Paweł Witkowski

Aleksandra Woźnica

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

- [x] Wyświetl książki
  - [x] Wyświetlaj BookEntries
  - [ ] BookEntries wg. usera
- [x] Dodaj książkę
- [x] Pokaż detale w prawym panelu
- [ ] Dodać edytowanie BookEntry w prawym panelu
- [ ] Dodać usuwanie BookEntry
- [ ] Dodać zapisywanie w LocalStorage dla guesta?
- [ ] Dodać filtrowanie książek

### UI

- [ ] Dodawanie książki
- [ ] Wyświetlić wszystkie pola z Book Entry i Book w detalach
- [ ] Dodać edytowanie BookEntry w prawym panelu
- [ ] Dodać usuwanie BookEntry
- [ ] Dodać okładki książek
- [ ] Dodać filtrowanie książek
- [ ] Dodać logowanie!!!!!!!

### Backend

- [ ] DB
- [ ] Przerzucić z repositories do DB

### API

- [x] Wybrać API
- [x] Dodać dodawanie przez API
  - [ ] Dodać wyszukiwanie za pomocą tytułu przez API?


## Jak uruchomić
1) Otwórz terminal, przejdź w nim do folderu z projektem, np. ```cd scalaproject```
2) Uruchom aplikację wpisując ```sbt run```
3) Otwórz przeglądarkę i wejdź na http://localhost:9000/
