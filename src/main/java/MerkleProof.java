package it.unicam.cs.asdl2425.mp1;

/**
 * Una classe che rappresenta una prova di Merkle per un determinato albero di
 * Merkle ed un suo elemento o branch. Oggetti di questa classe rappresentano un
 * proccesso di verifica auto-contenuto, dato da una sequenza di oggetti
 * MerkleProofHash che rappresentano i passaggi necessari per validare un dato
 * elemento o branch in un albero di Merkle decisi al momento di costruzione
 * della prova.
 *
 * @author Luca Tesei, Marco Caputo (template)
 *         Riccardo Catervi riccardo.catervi@studenti.unicam.it (implementazione)
 */
public class MerkleProof {

    /**
     * La prova di Merkle, rappresentata come una lista concatenata di oggetti
     * MerkleProofHash.
     */
    private final HashLinkedList<MerkleProofHash> proof;

    /**
     * L'hash della radice dell'albero di Merkle per il quale la prova è stata
     * costruita.
     */
    private final String rootHash;

    /**
     * Lunghezza massima della prova, dato dal numero di hash che la compongono
     * quando completa. Serve ad evitare che la prova venga modificata una volta
     * che essa sia stata completamente costruita.
     */
    private final int length;

    /**
     * Costruisce una nuova prova di Merkle per un dato albero di Merkle,
     * specificando la radice dell'albero e la lunghezza massima della prova. La
     * lunghezza massima della prova è il numero di hash che la compongono
     * quando completa, oltre il quale non è possibile aggiungere altri hash.
     *
     * @param rootHash
     *                     l'hash della radice dell'albero di Merkle.
     * @param length
     *                     la lunghezza massima della prova.
     */
    public MerkleProof(String rootHash, int length) {
        if (rootHash == null)
            throw new IllegalArgumentException("The root hash is null");
        this.proof = new HashLinkedList<>();
        this.rootHash = rootHash;
        this.length = length;
    }

    /**
     * Restituisce la massima lunghezza della prova, dato dal numero di hash che
     * la compongono quando completa.
     *
     * @return la massima lunghezza della prova.
     */
    public int getLength() {
        return length;
    }

    /**
     * Aggiunge un hash alla prova di Merkle, specificando se esso dovrebbe
     * essere concatenato a sinistra o a destra durante la verifica della prova.
     * Se la prova è già completa, ovvero ha già raggiunto il massimo numero di
     * hash deciso alla sua costruzione, l'hash non viene aggiunto e la funzione
     * restituisce false.
     *
     * @param hash
     *                   l'hash da aggiungere alla prova.
     * @param isLeft
     *                   true se l'hash dovrebbe essere concatenato a sinistra,
     *                   false altrimenti.
     * @return true se l'hash è stato aggiunto con successo, false altrimenti.
     */
    public boolean addHash(String hash, boolean isLeft) {
        // Se l'hash è nullo o si è già raggiunto il numero massimo di hash, ritorna false
        if (hash == null || this.proof.getSize() >= this.length)
            return false;

        // Crea un nuovo MerkleProofHash mediante l'hash e la posizione
        MerkleProofHash proofHash = new MerkleProofHash(hash, isLeft);
        // L'hash viene aggiunto alla coda della lista degli hash della prova
        this.proof.addAtTail(proofHash);

        // Ritorna true per segnalare che l'aggiunta dell'hash è andata a buon fine
        return true;
    }

    /**
     * Rappresenta un singolo step di una prova di Merkle per la validazione di
     * un dato elemento.
     */
    public static class MerkleProofHash {
        /**
         * L'hash dell'oggetto.
         */
        private final String hash;

        /**
         * Indica se l'hash dell'oggetto dovrebbe essere concatenato a sinistra
         * durante la verifica della prova.
         */
        private final boolean isLeft;

        public MerkleProofHash(String hash, boolean isLeft) {
            if (hash == null)
                throw new IllegalArgumentException("The hash cannot be null");

            this.hash = hash;
            this.isLeft = isLeft;
        }

        /**
         * Restituisce l'hash dell'oggetto MerkleProofHash.
         *
         * @return l'hash dell'oggetto MerkleProofHash.
         */
        public String getHash() {
            return hash;
        }

        /**
         * Restituisce true se, durante la verifica della prova, l'hash
         * dell'oggetto dovrebbe essere concatenato a sinistra, false
         * altrimenti.
         *
         * @return true se l'hash dell'oggetto dovrebbe essere concatenato a
         *         sinistra, false altrimenti.
         */
        public boolean isLeft() {
            return isLeft;
        }

        @Override
        public boolean equals(Object obj) {
            // Se l'oggetto corrente e quello passato sono uguali restituisce true
            if (this == obj)
                return true;

            // Se l'oggetto generico passato come argomento non è un'istanza di MerkleProofHash non possono essere uguali
            if (!(obj instanceof MerkleProofHash))
                return false;

            // Castiamo l'oggetto passato a MerkleProofHash per poi confrontare gli attributi
            MerkleProofHash other = (MerkleProofHash) obj;
            // Dopo aver confrontato gli hash e le posizioni ritorna il valore booleano risultante
            return this.hash.equals(other.hash) && this.isLeft == other.isLeft;
        }

        @Override
        public String toString() {
            return hash + (isLeft ? "L" : "R");
        }

        @Override
        // In accordo con equals, calcola l'hash code utilizzando l'hash dell'oggetto e la posizione
        public int hashCode() { return this.hash.hashCode() * 31 + (isLeft ? 101 : 103); }
    }

    /**
     * Valida un dato elemento per questa prova di Merkle. La verifica avviene
     * combinando l'hash del dato con l'hash del primo oggetto MerkleProofHash
     * in un nuovo hash, il risultato con il successivo e così via fino
     * all'ultimo oggetto, e controllando che l'hash finale coincida con quello
     * del nodo radice dell'albero di Merkle orginale.
     *
     * @param data
     *                 l'elemento da validare.
     * @return true se il dato è valido secondo la prova; false altrimenti.
     * @throws IllegalArgumentException
     *                                      se il dato è null.
     */
    public boolean proveValidityOfData(Object data) {
        // Verifica la validità del dato passato altrimenti lancia un'eccezione
        if (data == null)
            throw new IllegalArgumentException("Il dato non può essere null.");

        return obtainFinalHash(HashUtil.dataToHash(data)).equals(rootHash);
    }

    /**
     * Valida un dato branch per questa prova di Merkle. La verifica avviene
     * combinando l'hash del branch con l'hash del primo oggetto MerkleProofHash
     * in un nuovo hash, il risultato con il successivo e così via fino
     * all'ultimo oggetto, e controllando che l'hash finale coincida con quello
     * del nodo radice dell'albero di Merkle orginale.
     *
     * @param branch
     *                   il branch da validare.
     * @return true se il branch è valido secondo la prova; false altrimenti.
     * @throws IllegalArgumentException
     *                                      se il branch è null.
     */
    public boolean proveValidityOfBranch(MerkleNode branch) {
        // Verifica la validità del dato passato e lancia un'eccezione se il dato è nullo
        if (branch == null)
            throw new IllegalArgumentException("Il branch non può essere null.");

        // Verifica se il branch è una foglia
        if (branch.isLeaf()) {
            return obtainFinalHash(branch.getHash()).equals(rootHash);
        }

        // Costruisce un nuovo StringBuilder per aggiungere gli hash dei figli sx e dx del branch
        StringBuilder hashBuilder = new StringBuilder();
        hashBuilder.append(branch.getLeft() != null ? branch.getLeft().getHash() : "")
                .append(branch.getRight() != null ? branch.getRight().getHash() : "");

        // Calcola l'hash combinato dei figli
        String combinedHash = HashUtil.computeMD5(hashBuilder.toString().getBytes());

        // Verifica sia la correttezza dell'hash interno che la sua validità rispetto alla radice
        return combinedHash.equals(branch.getHash()) &&
                obtainFinalHash(branch.getHash()).equals(rootHash);
    }

    /**
     * Calcola l'hash finale attraverso una combinazione tra un hash iniziale con gli hash della prova.
     *
     * @param initialHash l'hash iniziale da cui partire.
     * @return l'hash finale risultante dalla combinazione.
     */
    private String obtainFinalHash(String initialHash) {
        // Assegna L'hash iniziale all'hash calcolato
        String workingHash = initialHash;

        // Si itera attraverso tutti gli hash presenti nella prova
        for (MerkleProofHash proofHash : proof) {
            // Crea un nuovo StringBuilder per costruire l'hash combinato
            StringBuilder hashBuilder = new StringBuilder();

            // Nel caso in cui l'hash sia il fratello sinistro, lo concatena prima dell'hash corrente
            if (proofHash.isLeft())
                hashBuilder.append(proofHash.getHash()).append(workingHash);
            // Altrimenti, se è il fratello è a destra, lo concatena dopo l'hash corrente
            else
                hashBuilder.append(workingHash).append(proofHash.getHash());

            // Attraverso la classe HashUtil si calcola il nuovo hash combinato
            workingHash = HashUtil.computeMD5(hashBuilder.toString().getBytes());
        }
        return workingHash;
    }

}
