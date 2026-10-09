# Compte-Rendu : TP n°3 - HTTP
**ARSIR - Polytech Lyon (4A)**  
**Enseignants :** M. MORGE & N. SALAZAR  
**Année :** 2026–2027  
**Groupe :** 12 - VOICU David et ZANNOUH Amel
---

## Sommaire
1. [Exercice 1 : Naviguez](#exercice-1--naviguez)
2. [Exercice 2 : Susurrez à l'oreille du serveur](#exercice-2--susurrez-à-loreille-du-serveur)
3. [Exercice 3 : Architecture du serveur HTTP](#exercice-3--architecture-du-serveur-http)
4. [Exercice 4 : Décoder des requêtes HTTP](#exercice-4--décoder-des-requêtes-http)
5. [Exercice 5 : Réponses du serveur - Les erreurs](#exercice-5--réponses-du-serveur---les-erreurs)
6. [Exercice 6 : Réponses du serveur - Les autres (Fichiers & Succès)](#exercice-6--réponses-du-serveur---les-autres)
7. [Exercice 7 : Virtualiser pour aller plus loin (Virtual Hosting)](#exercice-7--virtualiser-pour-aller-plus-loin)
8. [Guide d'exécution et de test](#guide-dexécution-et-de-test)

---

## Exercice 1 : Naviguez

### Q1. Quels sont les messages associés aux codes 200, 404, 418 et 502?

200 OK
304 Not Modified
404 Not Found
418 I'm a teapot
502 Bad Gateway

#### Messages associés aux codes demandés :
| Code HTTP | Message standard | Signification |
| :---: | :---: | :--- |
| **`200`** | **`OK`** | La requête a réussi. Le serveur renvoie la ressource demandée. |
| **`404`** | **`Not Found`** | La ressource demandée n'existe pas ou n'a pas été trouvée sur le serveur. |
| **`418`** | **`I'm a teapot`** | Code humoristique standardisé par l'IETF dans la **RFC 2324** (protocole HTCPCP - *Hyper Text Coffee Pot Control Protocol*, poisson d'avril 1998). Il indique que le serveur refuse de préparer du café car il s'agit d'une théière. |
| **`502`** | **`Bad Gateway`** | Erreur côté serveur : un serveur agissant comme passerelle (*gateway*) ou proxy a reçu une réponse invalide du serveur en amont. |

---

## Exercice 2 : Susurrez à l'oreille du serveur

### Q1. Envoi des requêtes avec Telnet vers `perdu.com 80`

#### 1. Requête : `GET /\r\n`
- **Réponse obtenue :** Selon le serveur et son niveau de compatibilité ascendante :
  - Soit le serveur supporte **HTTP/0.9** (la toute première version du protocole) et renvoie immédiatement le contenu brut HTML sans en-tête de réponse.
  - Soit le serveur moderne ou reverse proxy (ex. Cloudflare/nginx) refuse la requête et répond par une erreur `400 Bad Request`, car aucune version de protocole n'est spécifiée.
- **Explication :** La syntaxe sur une seule ligne `GET /` sans version de protocole correspond au standard historique HTTP/0.9.

#### 2. Requête : `GET / HTTP/1.1\r\n\r\n`
- **Réponse obtenue :** `HTTP/1.1 400 Bad Request`
- **Explication :** Dans la norme **HTTP/1.1** (RFC 2616 section 14.23, RFC 7230, RFC 9112), le champ d'en-tête **`Host:` est strictement obligatoire**. Toute requête HTTP/1.1 dépourvue de l'en-tête `Host:` est invalide et **doit** être rejetée par le serveur avec le code statut `400 Bad Request`.

#### 3. Requête : `GET / HTTP/1.1\r\nHost:perdu.com\r\n\r\n`
- **Réponse obtenue :** `HTTP/1.1 200 OK`, suivie des en-têtes HTTP de réponse (`Date`, `Server`, `Content-Type: text/html`, `Content-Length`, etc.), d'une ligne vide, puis du corps HTML de la page d'accueil.
- **Explication :** La requête est parfaitement formée et conforme à HTTP/1.1 (méthode, URL, version, en-tête `Host:`, et double saut de ligne `\r\n\r\n` marquant la fin des en-têtes).

#### À quoi sert le champ `Host:` ?
Le champ d'en-tête `Host:` indique le nom de domaine (FQDN) et le port de la ressource demandée. Il est fondamental pour le **Virtual Hosting** (hébergement virtuel par nom) :
- Une même machine physique et une même adresse IP peuvent héberger plusieurs sites web indépendants (ex. `site1.fr` et `site2.fr`) sur le même port TCP (80).
- Lors de l'établissement de la connexion TCP, le serveur ne connaît que son adresse IP locale et celle du client.
- C'est donc le champ `Host:` au niveau applicatif qui permet au serveur web de déterminer quel site virtuel le client souhaite consulter et de servir les fichiers depuis le bon répertoire racine.

---

## Exercice 3 : Architecture du serveur HTTP

### Q1. Arborescence de fichiers statiques
Nous avons mis en place une arborescence complète avec des pages HTML reliées entre elles :
```
tp3/
├── site1/
│   ├── index.html            (Accueil du site 1)
│   ├── page1.html            (Page secondaire avec lien retour)
│   ├── mon document.html     (Fichier avec espace pour tester le décodage %20)
│   ├── style.css             (Feuille de style CSS)
│   └── dossier/
│       ├── index.html        (Index du sous-dossier, testé pour l'exercice 6)
│       └── page2.html        (Page dans le sous-dossier avec liens)
└── site2/                    (Utilisé pour l'exercice 7 Virtual Hosting)
    ├── index.html            (Accueil du site virtuel 2)
    ├── contact.html          (Page contact du site 2)
    └── style.css             (Style personnalisé pour le site 2)
```

### Q2. Architecture logicielle
Le serveur repose sur :
- `ServerSocket(6666)` pour écouter les connexions entrantes sur le port 6666.
- Une boucle `while (true)` avec `socketServeur.accept()`.
- Un traitement **multi-threadé** : chaque client connecté est pris en charge par un thread dédié (`ClientHandler extends Thread`), assurant le traitement de requêtes simultanées en parallèle.
- `BufferedReader` pour lire la requête ligne par ligne.
- Gestion robuste des exceptions (`IOException`, `SocketException`) et fermeture systématique des ressources (`try ... catch ... finally`).

---

## Exercice 4 : Décoder des requêtes HTTP

### Q1. Méthode `recevoirRequete`
```java
public static String recevoirRequete(BufferedReader entree) throws IOException {
    StringBuilder requete = new StringBuilder();
    String ligne;
    while ((ligne = entree.readLine()) != null) {
        if (ligne.isEmpty()) {
            break; // La première ligne vide marque la fin des en-têtes HTTP
        }
        requete.append(ligne).append("\r\n");
    }
    return requete.toString();
}
```

### Q2. Méthode `verifierRequete` avec expressions régulières (`java.util.regex`)
L'expression régulière utilisée pour la ligne de requête est :
`^([A-Za-z]+)\s+(\S+)\s+HTTP/(\d+(?:\.\d+)?)$`
- Si la première ligne ne correspond pas au format attendu &rarr; **`400 Bad Request`**.
- Si le format est valide mais que la méthode n'est pas `GET` &rarr; **`405 Method Not Allowed`**.
- Si la méthode est `GET` et la syntaxe valide &rarr; **`200 OK`**.

---

## Exercice 5 : Réponses du serveur - Les erreurs

### Q1. Méthode surchargée `genererEnTete`
Génère l'en-tête de réponse HTTP selon les spécifications :
- Ligne de statut : `HTTP/1.1 <code> <message>\r\n` (dictionnaire `Map<Integer, String>`). Si le code est inconnu, l'erreur `500 Internal Server Error` est renvoyée par défaut.
- Date courante : format exact `SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.FRENCH)` (ex: `lun., 24 nov. 2025 09:45:39 CET`).
- Nom du serveur : `Server: ServeurHTTP-ARSIR/1.0`.
- Fermeture de connexion : `Connection: close`.
- `Content-Length:` uniquement présent si l'argument de taille a été fourni.
- `Content-Type: text/html` (ou enrichi selon le fichier).
- Ligne vide finale `\r\n`.

### Q2. Méthode `genererReponseErreur`
Génère une page HTML d'erreur complète et stylisée (titre, message, code d'erreur, signature du serveur), calcule sa taille en octets (`byte[]`), appelle `genererEnTete(code, taille)`, et concatène le tout suivi d'une ligne vide.

---

## Exercice 6 : Réponses du serveur - Les autres

### Q1. Méthode `construireCheminFichier`
Cette méthode prend en charge l'ensemble des cas particuliers :
1. **Éléments parasites après `?`** : suppression de tout ce qui suit le caractère `?` (inclus) grâce à `substring(0, indexInterrogation)`.
2. **Décodage des séquences `%xx`** : utilisation d'une regex `%([0-9a-fA-F]{2})` pour remplacer chaque code hexadécimal par le caractère ASCII correspondant (ex: `%20` devient un espace `' '`).
3. **Préfixe absolu** : suppression éventuelle de `http://...` si l'URL fournie est absolue.
4. **Dossiers et `index.html`** : si le chemin résolu pointe vers un dossier (`fichier.isDirectory()`), le serveur cible automatiquement `dossier/index.html`.
5. **Fichier inexistant** : si le fichier n'existe pas, la méthode ou le gestionnaire renvoie une erreur `404 Not Found`.

### Q2. Méthode `genererReponseSucces`
Envoie au client :
1. L'en-tête HTTP 200 OK avec le `Content-Length` calculé sur la taille exacte du fichier et le `Content-Type` adapté (`text/html`, `text/css`, etc.).
2. Le contenu binaire du fichier lu via un `FileInputStream` et écrit sur `socket.getOutputStream()`.

---

## Exercice 7 : Virtualiser pour aller plus loin

### Q1. Prise en charge de deux sites web via l'en-tête `Host:`
Le serveur implémente le **Virtual Hosting** :
- La méthode `extraireChampHost(String requete)` recherche l'en-tête `Host:`.
- **Si le champ `Host:` est absent** : le serveur répond par une erreur `400 Bad Request` ("Le champ d'en-tête 'Host:' est obligatoire").
- **Si `Host:` contient `site1` ou `site1.local` (ou `localhost`)** : le serveur oriente la requête vers le dossier `site1/`.
- **Si `Host:` contient `site2` ou `site2.local`** : le serveur oriente la requête vers le dossier `site2/`.
- **Si l'hôte virtuel est inconnu** : le serveur renvoie une erreur `404 Not Found`.

---

## Guide d'exécution et de test

### 1. Structure du projet Java
- `src/Main.java` : Menu principal permettant de démarrer le serveur (par défaut l'Exercice 7 complet).
- `src/exo3/` : `ServeurHTTP.java`, `ClientHTTP.java` (Exercice 3)
- `src/exo4/` : `ServeurHTTP.java`, `ClientHTTP.java` (Exercice 4)
- `src/exo5/` : `ServeurHTTP.java`, `ClientHTTP.java` (Exercice 5)
- `src/exo6/` : `ServeurHTTP.java`, `ClientHTTP.java` (Exercice 6)
- `src/exo7/` : `ServeurHTTP.java`, `ClientHTTP.java` (Exercice 7 - Virtual Hosting)

### 2. Compilation
Depuis le dossier `tp3/tp3` :
```powershell
javac -d out/production/tp3 -encoding UTF-8 (Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName })
```

### 3. Lancement du serveur complet
```powershell
java -cp out/production/tp3 Main
# ou directement :
java -cp out/production/tp3 exo7.ServeurHTTP
```

### 4. Tests
- **Dans un navigateur web :**
  - Ouvrir `http://localhost:6666/` pour naviguer sur le Site 1.
  - Tester les liens : `/page1.html`, `/dossier/`, `/mon%20document.html`, `/?param=valeur`.
  - Tester une page inexistante : `http://localhost:6666/fantome.html` &rarr; page d'erreur 404.
- **Avec les clients de test automatisés :**
  ```powershell
  java -cp out/production/tp3 exo4.ClientHTTP
  java -cp out/production/tp3 exo5.ClientHTTP
  java -cp out/production/tp3 exo6.ClientHTTP
  java -cp out/production/tp3 exo7.ClientHTTP
  ```
