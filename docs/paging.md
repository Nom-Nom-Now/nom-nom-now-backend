# Pagination – `GET /recipes`

Alle Rezepte werden paginiert zurückgegeben. Die Größe und Seite lassen sich per Query-Parameter steuern.

## Query-Parameter

| Parameter | Typ | Default | Min | Max | Beschreibung |
|-----------|-----|---------|-----|-----|--------------|
| `page`    | int | `0`     | `0` | –   | Seitennummer (**0-basiert**) |
| `size`    | int | `20`    | –   | `50`| Anzahl Einträge pro Seite |

## Beispiele

```
# Erste Seite, 20 Einträge (Default)
GET /recipes

# Erste Seite, 10 Einträge
GET /recipes?page=0&size=10

# Zweite Seite, 10 Einträge
GET /recipes?page=1&size=10

# Dritte Seite, 5 Einträge
GET /recipes?page=2&size=5
```

> ⚠️ `page` beginnt bei **0**, nicht bei 1.  
> ⚠️ `size` ist auf maximal **50** begrenzt.

## Response-Struktur

Die Antwort ist ein Spring-`Page`-Objekt:

```json
{
  "content": [ /* Array von Rezepten */ ],
  "totalElements": 42,
  "totalPages": 3,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

| Feld            | Beschreibung |
|-----------------|--------------|
| `content`       | Rezepte der aktuellen Seite |
| `totalElements` | Gesamtanzahl aller Rezepte |
| `totalPages`    | Gesamtanzahl der Seiten |
| `number`        | Aktuelle Seitennummer (0-basiert) |
| `size`          | Gewählte Seitengröße |
| `first` / `last`| Ob es die erste / letzte Seite ist |
