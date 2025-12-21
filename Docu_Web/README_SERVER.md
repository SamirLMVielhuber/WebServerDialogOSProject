# Erklärung

## Setup
1. Erstelle einen Keystore und füge ihn ins Projekt ein, merke Passwort
2. Suche IP Adresse heraus auf der das Programm laufen soll
3. Erstelle die File:
    <center>
    /com/server.properties<br>
    TODO Füge Example Picture ein
    </center>
    füge dort alles hinein. Die File ist aus Sicherheitsgründen im .gitignore, wir wollen ja nich das irgendwas ausversehen gepublisht wird.
    Dies gilt auch für den gesamten SSLCertificates Folder.
4. Füge in com.clt.webServer/src/main/resources/inf.json die Namen unter denen die Graphen zur verfügung stehen sollen und die Pfade zu den Graphen 

## Start
Der Command zum Starten des Online Services ist:
<center>
./gradlew :com.clt.webServer:testAudioServer
</center>

## Wie funktioniert's
### Online
**Website**
<div style="margin-left: 2em;">
        Zuerst wird ein Server gestartet auf Port 8080, dieser beinhaltet die Website.<br>
        Nach der Auswahl des Graphen, erstellt die Website eine random ID und leitet einen Weiter.<br>
        Diese userID zusammen mit dem gewählten Graphen wird dann im DialogLoadServlet genutzt um eine neue Connection zu etablieren.<br>
</div>
<br>

**ConnectionManager**
<div style="margin-left: 2em;">
    Der ConnectionManager beinhaltet die Connections, die userID und den dazu passenden GraphManager.<br>
    Der ConnectionManager lädt daraufhin den übergebenen Graphen mittels des GraphManager und weißt mit Hilfe des PortManager passende Port zu.<br>
    Der geladene Graph ist hierbei vom Typ WebDocument (Es können derzeit nur SingleDocuments geladen werden).<br>
    Der PortManager erlaubt bei der Portauswahl eine beliebige Anzahl X an Usern per Port (ist Hardcoded in der PortManagerklasse).<br>
    Die ausgewählten Ports (früher waren zwei unterschiedliche möglich, mittlerweile ist es der selbe Port für Input und Output) werden in den Plugins des geladenen Graphen gesetzt.<br>
    Die gewählten Ports und die IP-Adresse werden auch an die Website geschickt. Dadurch kann diese über Port XYZ und IP ABC mit den Endpoints connecten.<br>
    Der ConnectionManager setzt beim ersten Start ein GraphControlRegistry, dies gilt für die Nutzungszeit für alle Nutzer.<br>
    Das GraphControlRegistry erlaubt benutzung des DocumentManager und ConnectionManager innerhalb der Plugins.<br>
    Dadurch kann die komplette Kommunikation ausgelagert werden in das Plugin. Verbindungsabbrüche, Graphstart usw. können damit aus dem Plugin heraus über das ControlRegistry benutzt werden.
</div>
<br>

**Plugin**
<div style="margin-left: 2em;">
    - HubRegistry:
    <div style="margin-left: 2em;">
        Das Hubregistry hält alle aktiven ports in Verbindung mit dem passenden WebSocketHub.
    </div>
    - WebSocketHub:
    <div style="margin-left: 2em;">
        Ist der Server an einem Gewissen Port. <br>
        Er startet die Endpunkte "/audio-receive" (WebSocketAudioReceiver) und "/audio-stream" (AudioWebSocketStreamer).<br>
        Des Weiteren hält diese Klasse alle aktiven Sessions der UserId's die über den Port verbunden sind.<br>
        Ebenfalls hält sie callbacks für den Audio Input des Nutzers, um diesen an den richtigen Graphen weiterzuleiten.<br>       
    </div>
    - WebSockets:
    <div style="margin-left: 2em;">
        Es gibt zwei WebSockets, bei on Connect registrieren beide die Sessions für die passende userId.<br>
        Bei Disconnects unregistern sie ihre jeweiligen Sessions wieder.<br>
        WebSockets:<br>
        -- WebSocketAudioReceiver
        <div style="margin-left: 2em;">
            onMessage: 
        <div style="margin-left: 2em;">
                holt sich für die passende userId die   Callbackfunktion aus seinem WebSocketHub, und leitet das byteArray dorthin weiter.
        </div></div>
        -- WebSocketAudioStreamer
        <div style="margin-left: 2em;">
            onText: 
        <div style="margin-left: 2em;">
                da der Streamer eigentlich nichts bekommen sollte, wird der onText für Kontrollfunktionen genutzt.<br>
                Der gesendete Text "START_GRAPH", wird mittels des GraphControlRegistry zum Beispiel zum Starten des Graphen genutzt.
        </div></div>
    - Plugins:
    <div style="margin-left: 2em;">
        Es gibt zwei seperate Plugins eines für das handeln von Input und eines für das Handeln von Output.<br>
        -- WebSocketAudioInputPlugin:
        <div style="margin-left: 2em;">
            Wie oben beschrieben ruft die WebSocketAudioReceiver eine Callbackfunktion auf.<br>
            Diese Callbackfunktion stammt von dem zum Graph passenden WebSocketAudioInputPlugin.<br>
            Dies wird an einen WebSocketInputStream gehangen und dadurch an die Spracherkennung von DialogOS gegeben.
        </div>
        -- WebSocketAudioOutputPlugin:
        <div style="margin-left: 2em;">
            Da beide Plugins z.B. den WebSocketHub, und die userId kennen, kann einfach die passende Session für den Output aus dem WebSocketHub geholt werden.<br>
            Es wird dann ein WebSocketStreamer Thread gestartet der den Output and die passende Session streamt<br>
        </div></div></div><br>

**Bei Disconnects**
<div style="margin-left: 2em;">Die Sessions Disconnecten alle x-Minuten (falls kein Traffic darüber läuft), das ist gewollt um unnötiges blockieren von Resourcen zu verhindern.<br>
    Bei Disconnects zu einem der beiden Sessions. Wird ein UserTimeoutScheduler gestartet. Dieser wird nach x-Minuten ohne Verbindung einer der beiden Sessions für die userId den user Disconnecten, indem er die andere Session kappt, den Graph stoppt und die Callback rauswirft, auch wird ein Platz im Port wieder frei.<br>
    Für weitere Nutzung ist ein neu auswählen des Graphs nötig, also wieder zurück auf die Hauptseite (Der Nutzer wird derzeit leider noch nicht irgendwie darüber benachrichtigt).<br>
    </div>
</div>

