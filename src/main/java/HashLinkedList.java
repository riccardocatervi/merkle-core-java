package it.unicam.cs.asdl2425.mp1;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Una classe che rappresenta una lista concatenata con il calcolo degli hash
 * MD5 per ciascun elemento. Ogni nodo della lista contiene il dato originale di
 * tipo generico T e il relativo hash calcolato utilizzando l'algoritmo MD5.
 *
 * <p>
 * La classe supporta le seguenti operazioni principali:
 * <ul>
 * <li>Aggiungere un elemento in testa alla lista</li>
 * <li>Aggiungere un elemento in coda alla lista</li>
 * <li>Rimuovere un elemento dalla lista in base al dato</li>
 * <li>Recuperare una lista ordinata di tutti gli hash contenuti nella
 * lista</li>
 * <li>Costruire una rappresentazione testuale della lista</li>
 * </ul>
 *
 * <p>
 * Questa implementazione include ottimizzazioni come il mantenimento di un
 * riferimento all'ultimo nodo della lista (tail), che rende l'inserimento in
 * coda un'operazione O(1).
 *
 * <p>
 * La classe utilizza la classe HashUtil per calcolare l'hash MD5 dei dati.
 *
 * @param <T>
 *                il tipo generico dei dati contenuti nei nodi della lista.
 * 
 * @author Luca Tesei, Marco Caputo (template)
 *         Riccardo Catervi riccardo.catervi@studenti.unicam.it (implementazione)
 * 
 */
public class HashLinkedList<T> implements Iterable<T> {
    private Node head; // Primo nodo della lista

    private Node tail; // Ultimo nodo della lista

    private int size; // Numero di nodi della lista

    private int numeroModifiche; // Numero di modifiche effettuate sulla lista
                                 // per l'implementazione dell'iteratore
                                 // fail-fast

    /**
     * Costruisce una nuova lista concatenata vuota.
     */
    public HashLinkedList() {
        this.head = null;
        this.tail = null;
        this.size = 0;
        this.numeroModifiche = 0;
    }

    /**
     * Restituisce il numero attuale di nodi nella lista.
     *
     * @return il numero di nodi nella lista.
     */
    public int getSize() {
        return size;
    }

    /**
     * Rappresenta un nodo nella lista concatenata.
     */
    private class Node {
        String hash; // Hash del dato

        T data; // Dato originale

        Node next;

        /**
         * Costruisce un nuovo nodo con il dato specificato.
         * Calcola anche l'hash MD5 del dato utilizzando HashUtil.
         *
         * @param data il dato da inserire nel nodo.
         */
        Node(T data) {
            this.data = data;
            this.hash = HashUtil.dataToHash(data);
            this.next = null;
        }
    }

    /**
     * Aggiunge un nuovo elemento in testa alla lista.
     *
     * @param data il dato da aggiungere.
     * @throws NullPointerException se il dato passato è nullo.
     *
     */
    public void addAtHead(T data) {
        // Verifica che il dato non sia nullo, altrimenti lancia un'eccezione.
        verifyInputData(data, "Si sta tentando di aggiungere un elemento nullo in testa alla lista concatenata.");

        Node newNode = new Node(data); // Crea un nodo nuovo con il dato passato
        if (this.head == null)
            this.head = this.tail = newNode;
        else {
            // Oppure collega il nuovo nodo alla head corrente e ne aggiorna la variabile
            newNode.next = this.head;
            this.head = newNode;
        }

        // Incrementa i contatori dei nodi e del numero di modifiche effettuate fino a questo punto
        bumpCounters();
    }

    /**
     * Aggiunge un nuovo elemento in coda alla lista.
     *
     * @param data il dato da aggiungere.
     * @throws NullPointerException se il dato fornito è nullo.
     *
     */
    public void addAtTail(T data) {
        // Validazione del dato
        verifyInputData(data, "Si sta tentando di aggiungere un elemento nullo in coda alla lista concatenata.");

        Node newNode = new Node(data); // Crea un nuovo nodo con il dato fornito
        // In caso di lista vuota, head e tail faranno riferimento al nuovo nodo creato
        if (this.tail == null) {
            this.head = this.tail = newNode;
        }
        else {
            // Altrimenti il nuovo nodo viene collegato alla tail attuale e ne aggiorna la variabile di riferimento
            this.tail.next = newNode;
            this.tail = newNode;
        }

        // Incrementa i contatori size e numeroModifiche
        bumpCounters();
    }

    /**
     * Restituisce un'ArrayList contenente tutti gli hash nella lista in ordine.
     *
     * @return una lista con tutti gli hash della lista.
     */
    public ArrayList<String> getAllHashes() {
        // Crea una nuova ArrayList dovee inserire gli hash che ha come capacità iniziale la dimensione della HashLinkedList
        ArrayList<String> hashesList = new ArrayList<>(this.size);

        // Itera attraverso ogni nodo della HashLinkedList aggiungendo l'hash del nodo corrente alla ArrayList creata in precedenza.
        for (Node currentNode = this.head; currentNode != null; currentNode = currentNode.next) {
            hashesList.add(currentNode.hash);
        }

        return hashesList; // Restituisce la lista di tutti gli hash
    }

    /**
     * Costruisce una stringa contenente tutti i nodi della lista, includendo
     * dati e hash. La stringa dovrebbe essere formattata come nel seguente
     * esempio:
     * 
     * <pre>
     *     Dato: StringaDato1, Hash: 5d41402abc4b2a76b9719d911017c592
     *     Dato: StringaDato2, Hash: 7b8b965ad4bca0e41ab51de7b31363a1
     *     ...
     *     Dato: StringaDatoN, Hash: 2c6ee3d301aaf375b8f026980e7c7e1c
     * </pre>
     *
     * @return una rappresentazione testuale di tutti i nodi nella lista.
     */
    public String buildNodesString() {
        // Si controlla se la HashLinkedList è vuota, in caso positivo restituisce una stringa vuota
        if (this.head == null)
            return "";

        StringBuilder s = new StringBuilder(); // Viene creato uno StringBuilder per costruire la stringa da restituire

        // Si itera attraverso ogni nodo della HashLinkedList
        for (Node currentNode = this.head; currentNode != null; currentNode = currentNode.next) {
            s.append("Dato: ")
                    .append(currentNode.data)
                    .append(", Hash: ")
                    .append(currentNode.hash)
                    .append('\n'); // Separa i nodi con un carattere di nuova linea
        }

        return s.toString(); // Restituisce la stringa risultante
    }

    /**
     * Rimuove il primo elemento nella lista che contiene il dato specificato.
     *
     * @param data
     *                 il dato da rimuovere.
     * @return true se l'elemento è stato trovato e rimosso, false altrimenti.
     * @throws NullPointerException se il dato fornito è nullo.
     */
    public boolean remove(T data) {
        // Valida il dato
        verifyInputData(data, "Si sta tentando di rimuovere un elemento nullo dalla lista.");

        // Se stiamo tentando di rimuovere un elemento da una lista vuota
        if (this.head == null)
            return false; // Ritorna false per indicare che il dato non è stato trovato

        // Se stiamo tentando di rimuovere un elemento che si trova in testa alla lista
        if (this.head.data.equals(data)) {
            this.head = this.head.next; // La testa diventa quella del nodo successivo a quello che stiamo rimuovendo
            if (this.head == null)
                this.tail = null;

            //Vengono aggiornati i contatori
            this.size--;
            this.numeroModifiche++;

            return true; // Ritorna true per indicare che la rimozione è andata a buon fine
        }
        // Se stiamo tentando di rimuovere un elemento in un nodo qualsiasi
        for (Node previous = this.head, curr = this.head.next; curr != null; previous = curr, curr = curr.next) {
            // Se il dato del nodo corrente corrisponde al dato da rimuovere
            if (curr.data.equals(data)) {
                previous.next = curr.next; // Salta il nodo corrente collegando il precedente al successivo

                // Se il nodo corrente è l'ultimo nodo, ovvero la coda, aggiorna la coda
                if (curr == this.tail)
                    this.tail = previous;

                //Aggiorna i contatori e ritorna true per indicare che la rimozione è avvenuta
                this.size--;
                this.numeroModifiche++;
                return true;
            }
        }

        // Se stiamo tentando di rimuovere un dato non presente nella lista
        return false;
    }

    /**
     * Valida che il dato passato non sia nullo.
     *
     * @param data         il dato da validare.
     * @param errorMessage il messaggio di errore da utilizzare se il dato è null.
     * @throws NullPointerException se il dato è nullo.
     */
    private void verifyInputData(T data, String errorMessage) {
        Objects.requireNonNull(data, errorMessage);
    }

    /**
     * Incrementa il contatore dei nodi e il contatore delle modifiche correnti.
     */
    private void bumpCounters() {
        this.size++;
        this.numeroModifiche++;
    }

    /**
     * Restituisce un iteratore su questa LinkedList.
     *
     * @return un iteratore per scorrere gli elementi della lista concatenata.
     */
    @Override
    public Iterator<T> iterator() {
        return new Itr();
    }

    /**
     * Classe che realizza un iteratore fail-fast per HashLinkedList.
     */
    private class Itr implements Iterator<T> {
        private Node currentNode; // Nodo corrente
        private final int expectedModifications; // Numero di modifiche che ci si aspetta per il fail fast
        private boolean isValid; // Per verificare la validità dell'iteratore

        /**
         * Costruisce un nuovo iteratore prendendo come punto di inizio la testa della lista.
         *
         * @throws IllegalStateException se la lista concatenata si trova in uno stato non consentito.
         */
        private Itr() {
            this.currentNode = head; // Inizializzata con l'inizio della lista
            this.expectedModifications = numeroModifiche;
            this.isValid = true;
        }

        /**
         * Controlla la presenza di altri elementi nella lista.
         *
         * @return true se esiste un elemento successivo, false altrimenti.
         * @throws ConcurrentModificationException se la lista è stata modificata durante l'iterazione.
         * @throws IllegalStateException se l'iteratore non è più valido.
         */
        @Override
        public boolean hasNext() {
            ensureIteratorValidity(); // Verifica che non siano avvenute modifiche alla lista
            return this.currentNode != null; // Restituisce true se è presente un nodo successivo
        }

        /**
         * Restituisce l'elemento successivo nell'iterazione.
         *
         * @return il prossimo elemento
         * @throws IllegalStateException se l'iteratore non è più valido.
         * @throws ConcurrentModificationException se la lista è stata modificata
         *                          durante l'iterazione.
         * @throws NoSuchElementException se non ci sono ulteriori elementi.
         */
        @Override
        public T next() {
            ensureIteratorValidity(); // Verifica se sono avvenute modifiche

            // Se non sono presenti altri elementi lancia un'eccezione di tipo
            if (!hasNext())
                throw new NoSuchElementException("Non sono presenti ulteriori elementi nella lista concatenata.");

            try {
                T data = this.currentNode.data; // Recupera il dato del nodo attuale
                this.currentNode = this.currentNode.next; // Va al nodo successivo
                return data; // Ritorna il dato
            } catch (NullPointerException e) {
                // Se dovesse verificarsi un'eccezione di tipo NullPointerException, significa che la lista è stata modificata
                throw new ConcurrentModificationException("La lista concatenata è stata modificata durante l'iterazione", e);
            }
        }

        /**
         * Valida lo stato dell'iteratore e verifica la presenza di modifiche concorrenti.
         *
         * @throws ConcurrentModificationException se la lista è stata modificata durante l'iterazione.
         * @throws IllegalStateException se l'iteratore non è più valido.
         */
        private void ensureIteratorValidity() {
            //Se l'iteratore è stato invalidato lancia un'eccezione
            if (!this.isValid)
                throw new IllegalStateException("L'iteratore non è più valido.");

            // Se il numero di modifiche nella lista non corrisponde a quello atteso lancia un'eccezione
            if (this.expectedModifications != numeroModifiche) {
                disableIterator(); // L'iteratore viene invalidato
                throw new ConcurrentModificationException("La lista concatenata è stata modificata " +
                        "durante l'iterazione. '\n'Attese: " + expectedModifications + " modifiche. '\n'Rilevate: " +
                        numeroModifiche + " modifiche.");
            }
        }

        /**
         * Invalida l'iteratore per prevenire ed impedire utilizzi futuri.
         */
        private void disableIterator() {
            isValid = false;
            this.currentNode = null;
        }
    }
}