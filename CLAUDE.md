# CLAUDE.md

Regole da seguire per il codice Java di questo progetto.

## Package
- Ogni classe deve stare in un package, mai nel package predefinito (default package).

## Output su console
- L'output su console di un oggetto si fa con un metodo `visualizza()`.
- All'interno di una classe, l'unico metodo che stampa su console è `visualizza()`: nessun altro metodo della classe deve contenere `System.out.print*`.

## Setter
- I setter sono sempre `void`.
- I setter non stampano nulla su console.
