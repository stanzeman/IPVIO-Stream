package com.ipvio.stream

// T-072: "Continue watching" rules (remove after N days, settings in Storage > History).
// Format: key¦English¦Turkish¦Bosnian¦Croatian¦Serbian¦Spanish¦French¦Italian. Dutch, Arabic, Polish, Chinese and Portuguese: own columns below.
// Drafted, not reviewed by a native speaker.
val DictT072 = """
Nie¦Never¦Asla¦Nikad¦Nikad¦Nikad¦Nunca¦Jamais¦Mai
Nach {#0} Tagen¦After {#0} days¦{#0} gün sonra¦Nakon {#0} dana¦Nakon {#0} dana¦Nakon {#0} dana¦Después de {#0} días¦Après {#0} jours¦Dopo {#0} giorni
Filme aus Weiterschauen entfernen¦Remove movies from continue watching¦Filmleri izlemeye devam et listesinden kaldır¦Ukloni filmove iz nastavka gledanja¦Ukloni filmove iz nastavka gledanja¦Ukloni filmove iz nastavka gledanja¦Quitar películas de seguir viendo¦Retirer les films de la reprise de lecture¦Rimuovi i film da continua a guardare
Serien aus Weiterschauen entfernen¦Remove series from continue watching¦Dizileri izlemeye devam et listesinden kaldır¦Ukloni serije iz nastavka gledanja¦Ukloni serije iz nastavka gledanja¦Ukloni serije iz nastavka gledanja¦Quitar series de seguir viendo¦Retirer les séries de la reprise de lecture¦Rimuovi le serie da continua a guardare
Titel, die so lange nicht weitergeschaut wurden, verschwinden aus „Weiterschauen“. Ein Titel erscheint dort ab 5 Minuten und gilt ab 90 % oder 3 Minuten vor Schluss als fertig.¦Titles not continued for this long leave “Continue watching”. A title appears there after 5 minutes and counts as finished from 90 % or 3 minutes before the end.¦Bu süre boyunca devam edilmeyen başlıklar “İzlemeye devam et” listesinden çıkar. Bir başlık 5 dakikadan sonra orada görünür ve %90'dan ya da bitime 3 dakika kala bitmiş sayılır.¦Naslovi koji se toliko dugo ne nastave nestaju iz „Nastavi gledati“. Naslov se tamo pojavljuje nakon 5 minuta i smatra se završenim od 90 % ili 3 minute prije kraja.¦Naslovi koji se toliko dugo ne nastave nestaju iz „Nastavi gledati“. Naslov se tamo pojavljuje nakon 5 minuta i smatra se završenim od 90 % ili 3 minute prije kraja.¦Naslovi koji se toliko dugo ne nastave nestaju iz „Nastavi gledanje“. Naslov se tamo pojavljuje posle 5 minuta i smatra se završenim od 90 % ili 3 minuta pre kraja.¦Los títulos que no se continúan durante ese tiempo salen de «Seguir viendo». Un título aparece ahí a partir de 5 minutos y cuenta como terminado desde el 90 % o 3 minutos antes del final.¦Les titres non repris pendant ce temps quittent « Reprendre ». Un titre y apparaît après 5 minutes et compte comme terminé à partir de 90 % ou 3 minutes avant la fin.¦I titoli non ripresi per questo tempo escono da «Continua a guardare». Un titolo appare lì dopo 5 minuti e conta come finito dal 90 % o 3 minuti prima della fine.
abgelaufen: {0}¦expired: {0}¦süresi doldu: {0}¦isteklo: {0}¦isteklo: {0}¦isteklo: {0}¦caducado: {0}¦expiré : {0}¦scaduto: {0}
"""

val DictT072Nl = """
Nie¦Nooit
Nach {#0} Tagen¦Na {#0} dagen
Filme aus Weiterschauen entfernen¦Films uit verder kijken verwijderen
Serien aus Weiterschauen entfernen¦Series uit verder kijken verwijderen
Titel, die so lange nicht weitergeschaut wurden, verschwinden aus „Weiterschauen“. Ein Titel erscheint dort ab 5 Minuten und gilt ab 90 % oder 3 Minuten vor Schluss als fertig.¦Titels die zo lang niet verder bekeken zijn, verdwijnen uit „Verder kijken“. Een titel verschijnt daar na 5 minuten en telt als bekeken vanaf 90 % of 3 minuten voor het einde.
abgelaufen: {0}¦verlopen: {0}
"""

val DictT072Ar = """
Nie¦أبداً
Nach {#0} Tagen¦بعد {#0} أيام
Filme aus Weiterschauen entfernen¦إزالة الأفلام من متابعة المشاهدة
Serien aus Weiterschauen entfernen¦إزالة المسلسلات من متابعة المشاهدة
Titel, die so lange nicht weitergeschaut wurden, verschwinden aus „Weiterschauen“. Ein Titel erscheint dort ab 5 Minuten und gilt ab 90 % oder 3 Minuten vor Schluss als fertig.¦العناوين التي لم تُتابع طوال هذه المدة تختفي من «متابعة المشاهدة». يظهر العنوان هناك بعد 5 دقائق ويُعد منتهياً عند 90 % أو قبل النهاية بـ 3 دقائق.
abgelaufen: {0}¦انتهت المدة: {0}
"""

val DictT072Pl = """
Nie¦Nigdy
Nach {#0} Tagen¦Po {#0} dniach
Filme aus Weiterschauen entfernen¦Usuń filmy z „Oglądaj dalej”
Serien aus Weiterschauen entfernen¦Usuń seriale z „Oglądaj dalej”
Titel, die so lange nicht weitergeschaut wurden, verschwinden aus „Weiterschauen“. Ein Titel erscheint dort ab 5 Minuten und gilt ab 90 % oder 3 Minuten vor Schluss als fertig.¦Tytuły, których tak długo nie oglądano dalej, znikają z „Oglądaj dalej”. Tytuł pojawia się tam po 5 minutach i liczy się jako obejrzany od 90 % lub 3 minut przed końcem.
abgelaufen: {0}¦wygasło: {0}
"""

val DictT072Zh = """
Nie¦从不
Nach {#0} Tagen¦{#0} 天后
Filme aus Weiterschauen entfernen¦从继续观看中移除电影
Serien aus Weiterschauen entfernen¦从继续观看中移除剧集
Titel, die so lange nicht weitergeschaut wurden, verschwinden aus „Weiterschauen“. Ein Titel erscheint dort ab 5 Minuten und gilt ab 90 % oder 3 Minuten vor Schluss als fertig.¦在这段时间内未继续观看的内容会从“继续观看”中消失。观看 5 分钟后内容才会出现在那里，播放到 90 % 或距结尾 3 分钟时视为已看完。
abgelaufen: {0}¦已过期：{0}
"""

val DictT072Pt = """
Nie¦Nunca
Nach {#0} Tagen¦Após {#0} dias
Filme aus Weiterschauen entfernen¦Remover filmes de continuar assistindo
Serien aus Weiterschauen entfernen¦Remover séries de continuar assistindo
Titel, die so lange nicht weitergeschaut wurden, verschwinden aus „Weiterschauen“. Ein Titel erscheint dort ab 5 Minuten und gilt ab 90 % oder 3 Minuten vor Schluss als fertig.¦Títulos que não foram continuados por esse tempo saem de “Continuar assistindo”. Um título aparece ali após 5 minutos e conta como assistido a partir de 90 % ou 3 minutos antes do fim.
abgelaufen: {0}¦expirado: {0}
"""
