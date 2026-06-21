# Documentation Firestore - AndroGustine

Cette documentation décrit la structure Firestore actuellement exploitée par l'application copilote Android `AndroGustineCopilote`.

Elle peut servir de base à une application Web Angular spectateur.

## Vue Generale

Racine principale :

```text
raceSessions/{sessionId}
```

Sous-documents temps réel utilisés :

```text
raceSessions/{sessionId}/telemetry/latest
raceSessions/{sessionId}/track/current
raceSessions/{sessionId}/strategy/current
raceSessions/{sessionId}/instructions/current
```

## Selection De La Session Courante

L'application copilote lit la collection :

```text
raceSessions
```

avec la requête :

```text
orderBy(createdAtIso, desc)
limit(1)
```

La session la plus récente est donc celle qui possède le `createdAtIso` le plus récent.

## Document `raceSessions/{sessionId}`

Métadonnées de session.

| Champ | Type | Description |
|---|---:|---|
| `sessionId` | string | Identifiant logique de session |
| `createdAtIso` | string | Date de création ISO-8601 |
| `appRole` | string | Rôle applicatif producteur |
| `status` | string | État de session |
| `raceStarted` | boolean | Course démarrée ou non |
| `totalLaps` | number | Nombre total de tours |
| `trackName` | string | Nom du circuit |

Exemple :

```json
{
  "sessionId": "androgustine-session-2026-06-21T10-00-00",
  "createdAtIso": "2026-06-21T10:00:00Z",
  "appRole": "PILOT",
  "status": "RUNNING",
  "raceStarted": true,
  "totalLaps": 11,
  "trackName": "Silesia Ring"
}
```

## Document `raceSessions/{sessionId}/telemetry/latest`

Télémétrie courante, mise à jour en temps réel par l'application pilote.

| Champ | Type | Description |
|---|---:|---|
| `timestampIso` | string | Horodatage ISO-8601 de la télémétrie |
| `elapsedSessionS` | number | Temps écoulé session en secondes |
| `elapsedLapS` | number | Temps écoulé dans le tour courant en secondes |
| `currentLap` | number | Tour courant |
| `activeStrategy` | string | Stratégie active |
| `gpsLat` | number | Latitude GPS réelle du véhicule |
| `gpsLon` | number | Longitude GPS réelle du véhicule |
| `gpsSpeedKmh` | number | Vitesse GPS en km/h |
| `snappedDistanceM` | number | Distance projetée du véhicule sur le circuit |
| `ghostDistanceM` | number | Distance projetée de la Ghost Car sur le circuit |
| `deltaDistanceM` | number | Écart véhicule / Ghost en mètres |
| `heartRateBpm` | number | Fréquence cardiaque |
| `weatherTemperatureC` | number | Température météo en degres Celsius |
| `weatherWindKmh` | number | Vent en km/h |
| `weatherRainProbability` | number | Probabilité de pluie |
| `raceStarted` | boolean | Course démarrée ou non |

Exemple :

```json
{
  "timestampIso": "2026-06-21T10:05:30Z",
  "elapsedSessionS": 330.4,
  "elapsedLapS": 42.8,
  "currentLap": 2,
  "activeStrategy": "RACE",
  "gpsLat": 50.6841,
  "gpsLon": 18.6352,
  "gpsSpeedKmh": 85.4,
  "snappedDistanceM": 966.0,
  "ghostDistanceM": 1056.0,
  "deltaDistanceM": -90.0,
  "heartRateBpm": 99,
  "weatherTemperatureC": 28.4,
  "weatherWindKmh": 16.6,
  "weatherRainProbability": 0.0,
  "raceStarted": true
}
```

## Document `raceSessions/{sessionId}/track/current`

Circuit courant.

| Champ | Type | Description |
|---|---:|---|
| `trackName` | string | Nom du circuit |
| `totalDistanceM` | number | Longueur totale du circuit en mètres |
| `pointCount` | number | Nombre de points |
| `points` | array | Liste ordonnée ou triable des points du circuit |

Structure d'un point :

| Champ | Type | Description |
|---|---:|---|
| `distanceM` | number | Distance cumulée depuis le départ |
| `lat` | number | Latitude |
| `lon` | number | Longitude |

Exemple :

```json
{
  "trackName": "Silesia Ring",
  "totalDistanceM": 3636.0,
  "pointCount": 250,
  "points": [
    {
      "distanceM": 0.0,
      "lat": 50.6841,
      "lon": 18.6352
    },
    {
      "distanceM": 12.5,
      "lat": 50.6842,
      "lon": 18.6354
    }
  ]
}
```

## Document `raceSessions/{sessionId}/strategy/current`

Stratégie de course affichée sur la carte.

| Champ | Type | Description |
|---|---:|---|
| `startSegments` | array | Segments utilisés avant départ et au tour 1 |
| `raceSegments` | array | Segments utilisés à partir du tour 2 |

Règle de sélection des segments :

```text
currentLap == null -> startSegments
currentLap <= 1    -> startSegments
currentLap >= 2    -> raceSegments
```

Structure recommandée d'un segment :

| Champ | Type | Description |
|---|---:|---|
| `startDistanceM` | number | Début du segment en mètres |
| `endDistanceM` | number | Fin du segment en mètres |
| `color` | string | Couleur logique du segment |
| `label` | string | Libellé optionnel |

Couleurs recommandées :

```text
green
blue
yellow
white
```

Le copilote accepte aussi des variantes textuelles :

```text
GREEN / VERT
BLUE / BLEU
YELLOW / JAUNE
WHITE / BLANC
```

Exemple :

```json
{
  "startSegments": [
    {
      "startDistanceM": 0.0,
      "endDistanceM": 208.0,
      "color": "green",
      "label": "Depart"
    },
    {
      "startDistanceM": 966.0,
      "endDistanceM": 1056.0,
      "color": "blue",
      "label": "Gestion"
    }
  ],
  "raceSegments": [
    {
      "startDistanceM": 0.0,
      "endDistanceM": 300.0,
      "color": "yellow",
      "label": "Attention"
    }
  ]
}
```

Compatibilité parser copilote :

Le copilote lit prioritairement :

```text
startDistanceM
endDistanceM
color
label
```

Mais il tolère aussi certains alias pour les distances :

```text
fromDistanceM / toDistanceM
distanceStartM / distanceEndM
beginDistanceM / finishDistanceM
```

Et pour la couleur :

```text
color
segmentColor
colorKey
kind
type
```

## Document `raceSessions/{sessionId}/instructions/current`

Consignes envoyées par le copilote vers le pilote.

Pour une application Web spectateur, ce document est probablement à lire uniquement, sauf si l'interface Web doit aussi agir comme copilote.

| Champ | Type | Description |
|---|---:|---|
| `pilotPaceInstruction` | string | Consigne de cadence |
| `raceStatusInstruction` | string | Consigne d'état course |
| `pitStopRequest` | boolean | Demande d'arrêt stand |
| `updatedAtIso` | string | Horodatage ISO-8601 de l'envoi |
| `updatedBy` | string | Auteur de la consigne |

Valeurs autorisées pour `pilotPaceInstruction` :

```text
ACCELERATE
MAINTAIN
SLOW_DOWN
```

Valeurs autorisées pour `raceStatusInstruction` :

```text
RACE
NO_OVERTAKING
STOP
```

Valeur attendue pour `updatedBy` :

```text
COPILOT
```

Exemple :

```json
{
  "pilotPaceInstruction": "MAINTAIN",
  "raceStatusInstruction": "RACE",
  "pitStopRequest": false,
  "updatedAtIso": "2026-06-21T10:06:00Z",
  "updatedBy": "COPILOT"
}
```

## Etat De Connexion Firestore

Le copilote utilise les métadonnées Firestore pour différencier :

| État logique | Signification |
|---|---|
| `Connected` | Snapshot reçu depuis le serveur |
| `OfflineCache` | Données servies depuis le cache local |
| `Initializing` | Connexion en cours |
| `WaitingForTelemetry` | Session trouvée mais `telemetry/latest` absent |
| `NoSession` | Aucune session trouvée |
| `Error` | Erreur Firestore |
| `FirebaseNotInitialized` | Firebase non initialisé |

En Web Angular, l'équivalent important est de surveiller les snapshots Firestore et, si utile, leur provenance cache/serveur via les métadonnées.

## Recommandation Angular

### Listeners Temps Reel Recommandes

1. Chercher la dernière session :

```ts
query(
  collection(db, 'raceSessions'),
  orderBy('createdAtIso', 'desc'),
  limit(1)
)
```

2. Une fois `{sessionId}` connu, écouter :

```text
raceSessions/{sessionId}/telemetry/latest
raceSessions/{sessionId}/track/current
raceSessions/{sessionId}/strategy/current
raceSessions/{sessionId}/instructions/current
```

### Donnees Necessaires Pour L'affichage Spectateur

Pour un mix pilote/copilote, lire au minimum :

```text
raceSessions/{sessionId}
raceSessions/{sessionId}/telemetry/latest
raceSessions/{sessionId}/track/current
raceSessions/{sessionId}/strategy/current
raceSessions/{sessionId}/instructions/current
```

## Position Vehicule Et Ghost

### Vehicule Reel

Priorité recommandée :

```text
telemetry/latest.gpsLat
telemetry/latest.gpsLon
```

Fallback possible :

```text
telemetry/latest.snappedDistanceM
+ interpolation sur track/current.points
```

### Ghost Car

La position Ghost est calculée avec :

```text
telemetry/latest.ghostDistanceM
+ interpolation sur track/current.points
```

## Interpolation Sur Circuit

Principe utilisé :

1. Trier `track/current.points` par `distanceM`.
2. Trouver les deux points qui encadrent la distance cible.
3. Interpoler linéairement `lat` et `lon`.
4. Si la distance est avant le premier point : utiliser le premier point.
5. Si la distance dépasse la longueur totale : appliquer un modulo si `totalDistanceM` est connu.

Pseudo-code :

```ts
function positionAtDistance(points, distanceM, totalDistanceM) {
  if (!points.length || distanceM == null) return null;

  const sorted = [...points].sort((a, b) => a.distanceM - b.distanceM);

  const d =
    totalDistanceM && totalDistanceM > 0
      ? distanceM % totalDistanceM
      : distanceM;

  if (d <= sorted[0].distanceM) return sorted[0];
  if (d >= sorted[sorted.length - 1].distanceM) return sorted[sorted.length - 1];

  const nextIndex = sorted.findIndex(p => p.distanceM >= d);
  const prev = sorted[nextIndex - 1];
  const next = sorted[nextIndex];

  const span = next.distanceM - prev.distanceM;
  if (span <= 0) return prev;

  const ratio = (d - prev.distanceM) / span;

  return {
    distanceM: d,
    lat: prev.lat + (next.lat - prev.lat) * ratio,
    lon: prev.lon + (next.lon - prev.lon) * ratio
  };
}
```

## Projection Carte Canvas

La carte Canvas copilote utilise une projection locale métrique approximative :

```text
refLon = (minLon + maxLon) / 2
refLat = (minLat + maxLat) / 2
meanLatRad = radians(refLat)

x = (lon - refLon) * 111_320.0 * cos(meanLatRad)
y = (lat - refLat) * 110_540.0
```

Puis :

```text
scale = min(availableWidth / trackWidthM, availableHeight / trackHeightM)
```

L'axe Y est inversé pour l'affichage Canvas.

## Carte OpenStreetMap

La vue OSM copilote :

- affiche le fond OpenStreetMap ;
- trace le circuit complet depuis `track/current.points` ;
- trace les segments colorés depuis `strategy/current` ;
- affiche le marqueur véhicule ;
- affiche le marqueur Ghost ;
- cadre la carte uniquement sur les points du circuit.

Important :

```text
La bounding box OSM ne doit pas inclure la position GPS du téléphone spectateur/copilote.
Elle doit être calculée uniquement depuis track/current.points.
```

## Resume Des Chemins Firestore

```text
raceSessions/{sessionId}

raceSessions/{sessionId}/telemetry/latest

raceSessions/{sessionId}/track/current

raceSessions/{sessionId}/strategy/current

raceSessions/{sessionId}/instructions/current
```

## Notes Pour L'application Web Spectateur

- L'application Web peut être totalement read-only si elle ne pilote pas la course.
- Ne pas écrire dans `instructions/current` sauf si le Web doit devenir une interface copilote.
- Les données importantes changent en temps réel, surtout :
  - `telemetry/latest`
  - `instructions/current`
- `track/current` et `strategy/current` changent moins souvent, mais doivent rester écoutés pour supporter les imports/modifications de circuit ou stratégie.
- Prévoir des états vides :
  - aucune session ;
  - session sans télémétrie ;
  - circuit absent ;
  - points vides ;
  - stratégie absente ;
  - instructions absentes.
