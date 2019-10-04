
Hallo Matthias

* Wir verwenden noch eine sehr alte servlet api - Ich nehme an, man kann ohne Umstellung auf die aktuelle
Version wechseln
https://github.com/swissbib/supportiveServices/blob/change2Gradle/build.gradle#L11

* json-simple ist sehr alt (2012)
https://github.com/swissbib/supportiveServices/blob/change2Gradle/build.gradle#L13
https://mvnrepository.com/artifact/com.googlecode.json-simple/json-simple/1.1.1

Die nach meiner Kenntnis am häufigsten verwendete library für json Handling in Java ist Jackson
https://mvnrepository.com/search?q=jackson
Mit Gradle ist das handling von dependencies sehr einfach
Ich habe nicht mehr genau im Kopf, was wir alles mit json-simple machen. Ich vermute aber mal, dass sich der Aufwand in Grenzen hält

* handling der eigenen "unmanaged" dependency
https://github.com/swissbib/supportiveServices/blob/change2Gradle/build.gradle#L18
Wir haben, glaube ich, darüber schon gesprochen. Ich würde diese dependency entfernen und den wenigen code in die webapp integrieren,
wenn man ihn denn überhaupt noch braucht.

* alte commons-lang
https://mvnrepository.com/artifact/commons-lang/commons-lang/2.6
https://github.com/swissbib/supportiveServices/blob/change2Gradle/build.gradle#L16
diese ist noch von 2011
man kann auf die Version 3 aktualisieren (auskommentiert), muss dann aber wenige Dinge anpassen

* Ich habe die ganze Projektstruktur auf das von Gradle vorgeschlagene laypit angepasst, das sollte funktionieren
Die nicht in Git vorhandende Konfigurationsdatei
src/main/webapp/WEB-INF/classes/resources/services/mapping.librarysystem.xml
muss man per Hand umziehen

Durch das gradle plugin gretty
https://github.com/swissbib/supportiveServices/blob/change2Gradle/build.gradle#L3
kann man die Applikation einfach starten
Vorgehen nach dem Download
gradlew wrapper (das installiert die erforderliche Umgebung)
gradlew tasks
zeigt dann die möglichen tasks an 
In der Gruppe Gretty tasks sind dann alle gretty spezifischen tasks
Ich habe u.a. mal gradle tomcatRun benutzt
Man kann die Applikation dann mit 
http://localhost:8080/services/AvailabilityRequest?sysnumber=006014873&barcode=BM2001402&idls=DSV01&language=de
aufgerufen, bekomme aber einen internal server error
Das debugging habe ich auf die Schnelle noch nicht hinbekommen, man muss ein wenig in der intellij doku lesen

* Testing:
Jetzt, da wir ein up to date deployment tool benutzen könnten, ist Testing wieder ein Thema.
Ich werde in Zukunft versuchen darauf zu achten, dass in meinem Kontext kein Code mehr ohne Tests in die Produktion geht.
das war für mich auch die Hauptmotivation, die Umgebung für Dich auf Gradle umzustellen.
Bist Du dabei?

Günter
