package com.ipvio.stream

// T-063 to T-066: "Top 10 today" per movies/series, "Popular & new", "New titles", "Russian & Kazakh", series data.
// Format: key¦English¦Turkish¦Bosnian¦Croatian¦Serbian¦Spanish¦French¦Italian. Dutch, Arabic and Polish: own columns below.
// Drafted, not reviewed by a native speaker.
val DictT064 = """
Top 10 Filme heute¦Top 10 movies today¦Bugünün ilk 10 filmi¦Top 10 filmova danas¦Top 10 filmova danas¦Top 10 filmova danas¦Top 10 películas de hoy¦Top 10 films du jour¦Top 10 film di oggi
Top 10 Serien heute¦Top 10 series today¦Bugünün ilk 10 dizisi¦Top 10 serija danas¦Top 10 serija danas¦Top 10 serija danas¦Top 10 series de hoy¦Top 10 séries du jour¦Top 10 serie di oggi
Im Trend · Filme¦Trending · Movies¦Trendler · Filmler¦U trendu · Filmovi¦U trendu · Filmovi¦U trendu · Filmovi¦Tendencias · Películas¦Tendances · Films¦Di tendenza · Film
Im Trend · Serien¦Trending · Series¦Trendler · Diziler¦U trendu · Serije¦U trendu · Serije¦U trendu · Serije¦Tendencias · Series¦Tendances · Séries¦Di tendenza · Serie
Beliebt & neu¦Popular & new¦Popüler ve yeni¦Popularno i novo¦Popularno i novo¦Popularno i novo¦Populares y nuevas¦Populaires et récents¦Popolari e nuovi
Beliebt & neu · Filme¦Popular & new · Movies¦Popüler ve yeni · Filmler¦Popularno i novo · Filmovi¦Popularno i novo · Filmovi¦Popularno i novo · Filmovi¦Populares y nuevas · Películas¦Populaires et récents · Films¦Popolari e nuovi · Film
Beliebt & neu · Serien¦Popular & new · Series¦Popüler ve yeni · Diziler¦Popularno i novo · Serije¦Popularno i novo · Serije¦Popularno i novo · Serije¦Populares y nuevas · Series¦Populaires et récents · Séries¦Popolari e nuovi · Serie
Neue Titel¦New titles¦Yeni yapımlar¦Novi naslovi¦Novi naslovi¦Novi naslovi¦Títulos nuevos¦Nouveaux titres¦Nuovi titoli
Neue Titel · Filme¦New titles · Movies¦Yeni yapımlar · Filmler¦Novi naslovi · Filmovi¦Novi naslovi · Filmovi¦Novi naslovi · Filmovi¦Títulos nuevos · Películas¦Nouveaux titres · Films¦Nuovi titoli · Film
Neue Titel · Serien¦New titles · Series¦Yeni yapımlar · Diziler¦Novi naslovi · Serije¦Novi naslovi · Serije¦Novi naslovi · Serije¦Títulos nuevos · Series¦Nouveaux titres · Séries¦Nuovi titoli · Serie
Russisch & Kasachisch¦Russian & Kazakh¦Rusça ve Kazakça¦Rusko i kazahstansko¦Rusko i kazahstansko¦Rusko i kazahstansko¦Rusas y kazajas¦Russes et kazakhs¦Russi e kazaki
Russisch & Kasachisch · Filme¦Russian & Kazakh · Movies¦Rusça ve Kazakça · Filmler¦Rusko i kazahstansko · Filmovi¦Rusko i kazahstansko · Filmovi¦Rusko i kazahstansko · Filmovi¦Rusas y kazajas · Películas¦Russes et kazakhs · Films¦Russi e kazaki · Film
Russisch & Kasachisch · Serien¦Russian & Kazakh · Series¦Rusça ve Kazakça · Diziler¦Rusko i kazahstansko · Serije¦Rusko i kazahstansko · Serije¦Rusko i kazahstansko · Serije¦Rusas y kazajas · Series¦Russes et kazakhs · Séries¦Russi e kazaki · Serie
Die beliebtesten Filme und Serien des Tages¦The most popular movies and series of the day¦Günün en popüler film ve dizileri¦Najpopularniji filmovi i serije dana¦Najpopularniji filmovi i serije dana¦Najpopularniji filmovi i serije dana¦Las películas y series más populares del día¦Les films et séries les plus populaires du jour¦I film e le serie più popolari del giorno
Beliebte, gut bewertete Filme der letzten Jahre¦Popular, well-rated movies of recent years¦Son yılların popüler, yüksek puanlı filmleri¦Popularni, dobro ocijenjeni filmovi posljednjih godina¦Popularni, dobro ocijenjeni filmovi posljednjih godina¦Popularni, dobro ocenjeni filmovi poslednjih godina¦Películas populares y bien valoradas de los últimos años¦Films populaires et bien notés de ces dernières années¦Film popolari e ben valutati degli ultimi anni
Beliebte, gut bewertete Serien der letzten Jahre¦Popular, well-rated series of recent years¦Son yılların popüler, yüksek puanlı dizileri¦Popularne, dobro ocijenjene serije posljednjih godina¦Popularne, dobro ocijenjene serije posljednjih godina¦Popularne, dobro ocenjene serije poslednjih godina¦Series populares y bien valoradas de los últimos años¦Séries populaires et bien notées de ces dernières années¦Serie popolari e ben valutate degli ultimi anni
Oben findest du alle Filme, Neuheiten und Kategorien.¦At the top you'll find all movies, new additions and categories.¦Üstte tüm filmleri, yenilikleri ve kategorileri bulursun.¦Gore ćeš naći sve filmove, novitete i kategorije.¦Gore ćeš naći sve filmove, novitete i kategorije.¦Gore ćeš naći sve filmove, novitete i kategorije.¦Arriba encontrarás todas las películas, novedades y categorías.¦En haut, tu trouveras tous les films, les nouveautés et les catégories.¦In alto trovi tutti i film, le novità e le categorie.
Oben findest du alle Serien, Neuheiten und Kategorien.¦At the top you'll find all series, new additions and categories.¦Üstte tüm dizileri, yenilikleri ve kategorileri bulursun.¦Gore ćeš naći sve serije, novitete i kategorije.¦Gore ćeš naći sve serije, novitete i kategorije.¦Gore ćeš naći sve serije, novitete i kategorije.¦Arriba encontrarás todas las series, novedades y categorías.¦En haut, tu trouveras toutes les séries, les nouveautés et les catégories.¦In alto trovi tutte le serie, le novità e le categorie.
Serien-Daten werden ergänzt¦Completing series data¦Dizi verileri tamamlanıyor¦Dopunjavanje podataka o serijama¦Dopunjavanje podataka o serijama¦Dopunjavanje podataka o serijama¦Completando datos de series¦Complément des données des séries¦Completamento dati delle serie
"""

val DictT064Nl = """
Top 10 Filme heute¦Top 10 films van vandaag
Top 10 Serien heute¦Top 10 series van vandaag
Im Trend · Filme¦Trending · Films
Im Trend · Serien¦Trending · Series
Beliebt & neu¦Populair en nieuw
Beliebt & neu · Filme¦Populair en nieuw · Films
Beliebt & neu · Serien¦Populair en nieuw · Series
Neue Titel¦Nieuwe titels
Neue Titel · Filme¦Nieuwe titels · Films
Neue Titel · Serien¦Nieuwe titels · Series
Russisch & Kasachisch¦Russisch en Kazachs
Russisch & Kasachisch · Filme¦Russisch en Kazachs · Films
Russisch & Kasachisch · Serien¦Russisch en Kazachs · Series
Die beliebtesten Filme und Serien des Tages¦De populairste films en series van de dag
Beliebte, gut bewertete Filme der letzten Jahre¦Populaire, goed beoordeelde films van de laatste jaren
Beliebte, gut bewertete Serien der letzten Jahre¦Populaire, goed beoordeelde series van de laatste jaren
Oben findest du alle Filme, Neuheiten und Kategorien.¦Bovenaan vind je alle films, nieuwtjes en categorieën.
Oben findest du alle Serien, Neuheiten und Kategorien.¦Bovenaan vind je alle series, nieuwtjes en categorieën.
Serien-Daten werden ergänzt¦Seriegegevens worden aangevuld
"""

val DictT064Ar = """
Top 10 Filme heute¦أفضل 10 أفلام اليوم
Top 10 Serien heute¦أفضل 10 مسلسلات اليوم
Im Trend · Filme¦الرائج · أفلام
Im Trend · Serien¦الرائج · مسلسلات
Beliebt & neu¦رائج وجديد
Beliebt & neu · Filme¦رائج وجديد · أفلام
Beliebt & neu · Serien¦رائج وجديد · مسلسلات
Neue Titel¦عناوين جديدة
Neue Titel · Filme¦عناوين جديدة · أفلام
Neue Titel · Serien¦عناوين جديدة · مسلسلات
Russisch & Kasachisch¦روسي وكازاخي
Russisch & Kasachisch · Filme¦روسي وكازاخي · أفلام
Russisch & Kasachisch · Serien¦روسي وكازاخي · مسلسلات
Die beliebtesten Filme und Serien des Tages¦أكثر الأفلام والمسلسلات رواجاً اليوم
Beliebte, gut bewertete Filme der letzten Jahre¦أفلام رائجة وعالية التقييم من السنوات الأخيرة
Beliebte, gut bewertete Serien der letzten Jahre¦مسلسلات رائجة وعالية التقييم من السنوات الأخيرة
Oben findest du alle Filme, Neuheiten und Kategorien.¦ستجد في الأعلى جميع الأفلام والجديد والفئات.
Oben findest du alle Serien, Neuheiten und Kategorien.¦ستجد في الأعلى جميع المسلسلات والجديد والفئات.
Serien-Daten werden ergänzt¦جارٍ استكمال بيانات المسلسلات
"""

val DictT064Pl = """
Top 10 Filme heute¦Top 10 filmów dziś
Top 10 Serien heute¦Top 10 seriali dziś
Im Trend · Filme¦Na czasie · Filmy
Im Trend · Serien¦Na czasie · Seriale
Beliebt & neu¦Popularne i nowe
Beliebt & neu · Filme¦Popularne i nowe · Filmy
Beliebt & neu · Serien¦Popularne i nowe · Seriale
Neue Titel¦Nowe tytuły
Neue Titel · Filme¦Nowe tytuły · Filmy
Neue Titel · Serien¦Nowe tytuły · Seriale
Russisch & Kasachisch¦Rosyjskie i kazachskie
Russisch & Kasachisch · Filme¦Rosyjskie i kazachskie · Filmy
Russisch & Kasachisch · Serien¦Rosyjskie i kazachskie · Seriale
Die beliebtesten Filme und Serien des Tages¦Najpopularniejsze filmy i seriale dnia
Beliebte, gut bewertete Filme der letzten Jahre¦Popularne, dobrze oceniane filmy z ostatnich lat
Beliebte, gut bewertete Serien der letzten Jahre¦Popularne, dobrze oceniane seriale z ostatnich lat
Oben findest du alle Filme, Neuheiten und Kategorien.¦Na górze znajdziesz wszystkie filmy, nowości i kategorie.
Oben findest du alle Serien, Neuheiten und Kategorien.¦Na górze znajdziesz wszystkie seriale, nowości i kategorie.
Serien-Daten werden ergänzt¦Uzupełnianie danych seriali
"""
