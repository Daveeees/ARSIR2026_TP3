# Compte-Rendu : TP n°3 - HTTP
**ARSIR - Polytech Lyon (4A)**  
**Enseignants :** M. MORGE & N. SALAZAR  
**Année :** 2026–2027  
**Groupe :** 12 - VOICU David et ZANNOUH Amel
---

## Exercice 1 : Naviguez

### Q1. Quels sont les messages associés aux codes 200, 404, 418 et 502?

```
200 OK : requête réussi 
304 Not Modified
404 Not Found : la ressource demandée n'existe pas ou n'a pas était trouvé
418 I'm a teapot : blague de poisson d'avril
502 Bad Gateway : erreur côté serveur 
```
---

## Exercice 2 : Susurrez à l'oreille du serveur

### Q1. Envoi des requêtes avec Telnet vers `perdu.com 80`


#### 1. Requête : `GET /\r\n`
![img_1.png](img_1.png)

Réponse : error code: 1003
Explication : Aucun nom de site n'est donné, Cloudflare considère la requête comme un accès direct à son IP et la refuse.

#### 2. Requête : `GET / HTTP/1.1\r\n\r\n`
![img.png](img.png)

Réponse : 400 Bad Request
Explication : Le champ d'en-tête "Host:" est obligatoire. Son absence entraîne une erreur 400.

#### 3. Requête : `GET / HTTP/1.1\r\nHost:perdu.com\r\n\r\n`
![img_2.png](img_2.png)

 Réponse : 200 OK (avec les en-têtes de réponse et le contenu HTML de la page.)
 Explication : Requête HTTP/1.1 complète et bien formée.

à quoi sert le champ `Host:` ?
Le champ Host: permet d'identifier le nom de domaine du site demandé,
lorsque plusieurs sites partagent la même adresse IP. Il est obligatoire en HTTP/1.1.


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
