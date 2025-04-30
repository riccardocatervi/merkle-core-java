package it.unicam.cs.asdl2425.mp1;

import java.util.*;

/**
 * Un Merkle Tree, noto anche come hash tree binario, è una struttura dati per
 * verificare in modo efficiente l'integrità e l'autenticità dei dati
 * all'interno di un set di dati più ampio. Viene costruito eseguendo l'hashing
 * ricorsivo di coppie di dati (valori hash crittografici) fino a ottenere un
 * singolo hash root. In questa implementazione la verifica di dati avviene
 * utilizzando hash MD5.
 * 
 * @author Luca Tesei, Marco Caputo (template)
 *         Riccardo Catervi riccardo.catervi@studenti.unicam.it (implementazione)
 *
 * @param <T>
 *                il tipo di dati su cui l'albero è costruito.
 */
public class MerkleTree<T> {
    /**
     * Nodo radice dell'albero.
     */
    private final MerkleNode root;

    /**
     * Larghezza dell'albero, ovvero il numero di nodi nell'ultimo livello.
     */
    private final int width;

    /**
     * Altezza dell'albero, ovvero il numero di livelli dall'alto alla base.
     */
    private final int height;

    /**
     * Mappa che associa ogni hash alle liste dei suoi indici nell'ultimo livello.
     */
    private final Map<String, List<Integer>> hashIndexMap = new HashMap<>();

    /**
     * Set che contiene tutti gli hash presenti nell'albero.
     */
    private final Set<String> allHashCollection = new HashSet<>();

    /**
     * Mappa che associa ogni nodo foglia al numero di foglie nel suo sottoalbero.
     */
    private final Map<MerkleNode, Integer> leafCountMap = new HashMap<>();

    /**
     * Costruisce un albero di Merkle a partire da un oggetto HashLinkedList,
     * utilizzando direttamente gli hash presenti nella lista per costruire le
     * foglie. Si noti che gli hash dei nodi intermedi dovrebbero essere
     * ottenuti da quelli inferiori concatenando hash adiacenti due a due e
     * applicando direttmaente la funzione di hash MD5 al risultato della
     * concatenazione in bytes.
     *
     * @param hashList
     *                     un oggetto HashLinkedList contenente i dati e i
     *                     relativi hash.
     * @throws IllegalArgumentException
     *                                      se la lista è null o vuota.
     */
    public MerkleTree(HashLinkedList<T> hashList) {
        // Assicura che hashList non sia null e che contenga almeno un elemento, lanciando un eccezione in caso contrario
        if (hashList == null || hashList.getSize() == 0)
            throw new IllegalArgumentException("La lista concatenata non può essere nulla o vuota.");

        // Recupera tutti gli hash dalla lista
        List<String> hashes = hashList.getAllHashes();

        // Costruisce l'albero di Merkle con gli hash recuperati
        this.root = generateNewTree(hashes);
        // Imposta la larghezza dell'albero di Merkle come il numero di hash nella lista iniziale
        this.width = hashes.size();
        // Calcola l'altezza dell'albero di Merkle
        this.height = determineHeight(root);
    }

    /**
     * Restituisce il nodo radice dell'albero.
     *
     * @return il nodo radice.
     */
    public MerkleNode getRoot() {
        return root;
    }

    /**
     * Restituisce la larghezza dell'albero.
     *
     * @return la larghezza dell'albero.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Restituisce l'altezza dell'albero.
     *
     * @return l'altezza dell'albero.
     */
    public int getHeight() { return height; }

    /**
     * Restituisce l'indice di un dato elemento secondo l'albero di Merkle
     * descritto da un dato branch. Gli indici forniti partono da 0 e
     * corrispondono all'ordine degli hash corrispondenti agli elementi
     * nell'ultimo livello dell'albero da sinistra a destra. Nel caso in cui il
     * branch fornito corrisponda alla radice di un sottoalbero, l'indice
     * fornito rappresenta un indice relativo a quel sottoalbero, ovvero un
     * offset rispetto all'indice del primo elemento del blocco di dati che
     * rappresenta. Se l'hash dell'elemento non è presente come dato
     * dell'albero, viene restituito -1.
     *
     * @param branch
     *                   la radice dell'albero di Merkle.
     * @param data
     *                   l'elemento da cercare.
     * @return l'indice del dato nell'albero; -1 se l'hash del dato non è
     *         presente.
     * @throws IllegalArgumentException
     *                                      se il branch o il dato sono null o
     *                                      se il branch non è parte
     *                                      dell'albero.
     */
    public int getIndexOfData(MerkleNode branch, T data) {
        // Verifica che gli argomenti forniti non siano nulli
        if (branch == null || data == null)
            throw new IllegalArgumentException("Branch e dato non possono essere nulli.");

        // Verifica l'appartenza del branch all'albero
        if (!validateBranch(branch))
            throw new IllegalArgumentException("Il branch non fa parte dell'albero.");

        // Il dato viene convertito in un hash con la classe HashUtil
        String referenceHash = HashUtil.dataToHash(data);
        // Trova l'indice dell'hash nel branch specificato come argomento
        return getIndexInBranch(branch, referenceHash, 0);
    }

    /**
     * Restituisce l'indice di un elemento secondo questo albero di Merkle. Gli
     * indici forniti partono da 0 e corrispondono all'ordine degli hash
     * corrispondenti agli elementi nell'ultimo livello dell'albero da sinistra
     * a destra (e quindi l'ordine degli elementi forniti alla costruzione). Se
     * l'hash dell'elemento non è presente come dato dell'albero, viene
     * restituito -1.
     *
     * @param data
     *                 l'elemento da cercare.
     * @return l'indice del dato nell'albero; -1 se il dato non è presente.
     * @throws IllegalArgumentException
     *                                      se il dato è null.
     */
    public int getIndexOfData(T data) {
        // Verifica validità argomento
        if (data == null)
            throw new IllegalArgumentException("Il dato non può essere nullo.");

        // Converte il dato in un hash
        String referenceHash = HashUtil.dataToHash(data);
        // Recupera la lista di indici associati a questo hash specifico
        List<Integer> hashIndexList = hashIndexMap.get(referenceHash);
        // Se esiste almeno un indice, restituisce il primo. In caso contrario restituisce -1
        return (hashIndexList != null && !hashIndexList.isEmpty()) ? hashIndexList.get(0) : -1;
    }

    /**
     * Sottopone a validazione un elemento fornito per verificare se appartiene
     * all'albero di Merkle, controllando se il suo hash è parte dell'albero
     * come hash di un nodo foglia.
     *
     * @param data
     *                 l'elemento da validare
     * @return true se l'hash dell'elemento è parte dell'albero; false
     *         altrimenti.
     */
    public boolean validateData(T data) {
        // Verifica dell'argomento
        if (data == null) return false;

        // Converte il dato in un hash
        String referenceHash = HashUtil.dataToHash(data);
        // Verifica se la mappa contiene l'hash
        return hashIndexMap.containsKey(referenceHash);
    }

    /**
     * Sottopone a validazione un dato sottoalbero di Merkle, corrispondente
     * quindi a un blocco di dati, per verificare se è valido rispetto a questo
     * albero e ai suoi hash. Un sottoalbero è valido se l'hash della sua radice
     * è uguale all'hash di un qualsiasi nodo intermedio di questo albero. Si
     * noti che il sottoalbero fornito può corrispondere a una foglia.
     *
     * @param branch
     *                   la radice del sottoalbero di Merkle da validare.
     * @return true se il sottoalbero di Merkle è valido; false altrimenti.
     */
    public boolean validateBranch(MerkleNode branch) {
        // Verifica del dato passato
        if (branch == null) return false;

        //Verifica la presenza dell'hash passato nel set di tutti gli hash
        return allHashCollection.contains(branch.getHash());
    }

    /**
     * Sottopone a validazione un dato albero di Merkle per verificare se è
     * valido rispetto a questo albero e ai suoi hash. Grazie alle proprietà
     * degli alberi di Merkle, ciò può essere fatto in tempo costante.
     *
     * @param otherTree
     *                      il nodo radice dell'altro albero di Merkle da
     *                      validare.
     * @return true se l'altro albero di Merkle è valido; false altrimenti.
     * @throws IllegalArgumentException
     *                                      se l'albero fornito è null.
     */
    public boolean validateTree(MerkleTree<T> otherTree) {
        // Verifica del dato passato
        if (otherTree == null)
            throw new IllegalArgumentException("L'albero da validare non può essere nullo.");

        // Confronta l'hash della radice dei due alberi e ritorna il valore booleano risultante
        return root.getHash().equals(otherTree.getRoot().getHash());
    }

    /**
     * Trova gli indici degli elementi di dati non validi (cioè con un hash
     * diverso) in un dato Merkle Tree, secondo questo Merkle Tree. Grazie alle
     * proprietà degli alberi di Merkle, ciò può essere fatto confrontando gli
     * hash dei nodi interni corrispondenti nei due alberi. Ad esempio, nel caso
     * di un singolo dato non valido, verrebbe percorso un unico cammino di
     * lunghezza pari all'altezza dell'albero. Gli indici forniti partono da 0 e
     * corrispondono all'ordine degli elementi nell'ultimo livello dell'albero
     * da sinistra a destra (e quindi l'ordine degli elementi forniti alla
     * costruzione). Se l'albero fornito ha una struttura diversa, possibilmente
     * a causa di una quantità diversa di elementi con cui è stato costruito e,
     * quindi, non rappresenta gli stessi dati, viene lanciata un'eccezione.
     *
     * @param otherTree
     *                      l'altro Merkle Tree.
     * @throws IllegalArgumentException
     *                                      se l'altro albero è null o ha una
     *                                      struttura diversa.
     * @return l'insieme di indici degli elementi di dati non validi.
     */
    public Set<Integer> findInvalidDataIndices(MerkleTree<T> otherTree) {
        // Verifica che il dato non sia nullo
        if (otherTree == null)
            throw new IllegalArgumentException("L'albero fornito non può essere nullo.");
        // Verifica che la larghezza e l'altezza degli alberi siano identiche
        if (otherTree.getWidth() != width || otherTree.getHeight() != height)
            throw new IllegalArgumentException("L'albero fornito ha una struttura diversa.");

        // Crea un insieme per memorizzare gli indici invalidi
        Set<Integer> invalidIndicesSet = new HashSet<>();
        // Effettua una ricerca ricorsiva degli indici non validi
        getInvalidIndices(this.root, otherTree.getRoot(), 0, width - 1, invalidIndicesSet);
        // Ritorna l'insieme degli indici non validi
        return invalidIndicesSet;
    }

    /**
     * Restituisce la prova di Merkle per un dato elemento, ovvero la lista di
     * hash dei nodi fratelli di ciascun nodo nel cammino dalla radice a una
     * foglia contenente il dato. La prova di Merkle dovrebbe fornire una lista
     * di oggetti MerkleProofHash tale per cui, combinando l'hash del dato con
     * l'hash del primo oggetto MerkleProofHash in un nuovo hash, il risultato
     * con il successivo e così via fino all'ultimo oggetto, si possa ottenere
     * l'hash del nodo padre dell'albero. Nel caso in cui non ci, in determinati
     * step della prova non ci siano due hash distinti da combinare, l'hash deve
     * comunque ricalcolato sulla base dell'unico hash disponibile.
     *
     * @param data
     *                 l'elemento per cui generare la prova di Merkle.
     * @return la prova di Merkle per il dato.
     * @throws IllegalArgumentException
     *                                      se il dato è null o non è parte
     *                                      dell'albero.
     */
    public MerkleProof getMerkleProof(T data) {
        // Verifica del dato passato
        if (data == null)
            throw new IllegalArgumentException("Il dato non può essere nullo.");

        // Il dato viene convertito in hash
        String referenceHash = HashUtil.dataToHash(data);
        // Viene creato un nuovo MerkleProof con l'hash della radice e l'altezza dell'albero
        MerkleProof newProof = new MerkleProof(root.getHash(), height);
        // Costruisce MerkleProof per il dato specificato
        if (!generateProofForData(root, referenceHash, newProof))
            throw new IllegalArgumentException("Il dato non è presente nell'albero.");
        return newProof; // Ritorna la nuova prova costruita
    }

    /**
     * Restituisce la prova di Merkle per un dato branch, ovvero la lista di
     * hash dei nodi fratelli di ciascun nodo nel cammino dalla radice al dato
     * nodo branch, rappresentativo di un blocco di dati. La prova di Merkle
     * dovrebbe fornire una lista di oggetti MerkleProofHash tale per cui,
     * combinando l'hash del branch con l'hash del primo oggetto MerkleProofHash
     * in un nuovo hash, il risultato con il successivo e così via fino
     * all'ultimo oggetto, si possa ottenere l'hash del nodo padre dell'albero.
     * Nel caso in cui non ci, in determinati step della prova non ci siano due
     * hash distinti da combinare, l'hash deve comunque ricalcolato sulla base
     * dell'unico hash disponibile.
     *
     * @param branch
     *                   il branch per cui generare la prova di Merkle.
     * @return la prova di Merkle per il branch.
     * @throws IllegalArgumentException
     *                                      se il branch è null o non è parte
     *                                      dell'albero.
     */
    public MerkleProof getMerkleProof(MerkleNode branch) {
        if (branch == null)
            throw new IllegalArgumentException("Il branch non può essere null");
        if (!validateBranch(branch))
            throw new IllegalArgumentException("Il branch non fa parte dell'albero");

        String referenceHash = branch.getHash();
        // Calcola la profondità del branch dell'albero
        int branchDepth = determineDepth(root, branch, 0);
        MerkleProof newProof = new MerkleProof(root.getHash(), height - branchDepth);
        if (!generateProofForBranch(root, referenceHash, newProof))
            throw new IllegalArgumentException("Non è possibile costruire la Merkle Proof per il branch.");
        return newProof;
    }

    /**
     * Calcola la profondità di un nodo reference nell'albero di Merkle.
     *
     * @param current      il nodo corrente durante la ricerca.
     * @param reference       il nodo reference di cui calcolare la profondità.
     * @param currentDepth la profondità attuale durante la ricorsione.
     * @return la profondità del nodo reference, o -1 se non trovato.
     */
    private int determineDepth(MerkleNode current, MerkleNode reference, int currentDepth) {
        if (current == null) return -1;
        // Se il nodo corrente è il nodo referente, ritorna la profondità attuale
        if (current == reference) return currentDepth;

        // Ricerca ricorsiva nel sottoalbero sinistro
        int depth = determineDepth(current.getLeft(), reference, currentDepth + 1);
        if (depth != -1) return depth; // Se trovato nel sinistro, ritorna la profondità
        // Ricerca ricorsiva nel sottoalbero destro
        return determineDepth(current.getRight(), reference, currentDepth + 1);
    }

    /**
     * Calcola l'altezza dell'albero di Merkle.
     *
     * @param node il nodo corrente.
     * @return l'altezza del nodo.
     */
    private int determineHeight(MerkleNode node) {
        // Se il nodo è nullo, ritorna -1
        if (node == null) return -1;

        // Calcola l'altezza massima tra i sottoalberi sinistro e destro e aggiunge 1
        return 1 + Math.max(determineHeight(node.getLeft()),
                determineHeight(node.getRight()));
    }

    /**
     * Costruisce l'albero di Merkle a partire da una lista di hash.
     *
     * @param hashes la lista di hash delle foglie.
     * @return il nodo radice dell'albero di Merkle.
     * @throws IllegalArgumentException se la lista di hash è null o vuota.
     */
    private MerkleNode generateNewTree(List<String> hashes) {
        // Verifica che la lista di hash non sia vuota o null
        if (hashes == null || hashes.isEmpty())
            throw new IllegalArgumentException("Lista hash non valida");

        // Conversione diretta a array per performance
        String[] hashArray = hashes.toArray(new String[0]);

        // Creazione delle foglie
        List<MerkleNode> currentLevel = new ArrayList<>();
        for (int i = 0; i < hashArray.length; i++) {
            MerkleNode leafNode = new MerkleNode(hashArray[i]);
            currentLevel.add(leafNode);
            leafCountMap.put(leafNode, 1);
            allHashCollection.add(hashArray[i]);

            // Indicizzazione degli hash delle foglie
            hashIndexMap.computeIfAbsent(hashArray[i], k -> new ArrayList<>()).add(i);
        }

        // Costruzione iterativa dell'albero
        while (currentLevel.size() > 1) {
            List<MerkleNode> nextLevel = new ArrayList<>();

            for (int i = 0; i < currentLevel.size(); i += 2) {
                MerkleNode left = currentLevel.get(i);
                MerkleNode right = (i + 1 < currentLevel.size()) ? currentLevel.get(i + 1) : null;

                String combinedHash = generateCombinedHash(left, right);
                MerkleNode parentNode = new MerkleNode(combinedHash, left, right);

                nextLevel.add(parentNode);
                allHashCollection.add(combinedHash);

                // Aggiorna il conteggio delle foglie
                int leftCount = leafCountMap.getOrDefault(left, 0);
                int rightCount = (right != null) ? leafCountMap.getOrDefault(right, 0) : 0;
                leafCountMap.put(parentNode, leftCount + rightCount);
            }

            currentLevel = nextLevel;
        }

        return currentLevel.get(0);
    }


    /**
     * Combina gli hash di due nodi e calcola il nuovo hash.
     *
     * @param left  il nodo sinistro.
     * @param right il nodo destro.
     * @return l'hash combinato.
     */
    private String generateCombinedHash(MerkleNode left, MerkleNode right) {
        if (right == null) {
            return HashUtil.computeMD5(left.getHash().getBytes());
        }
        return HashUtil.computeMD5((left.getHash() + right.getHash()).getBytes());
    }

    /**
     * Restituisce il numero di foglie in un sottoalbero.
     *
     * @param node il nodo di cui ottenere il conteggio delle foglie.
     * @return il numero di foglie.
     */
    private int getNumberOfLeaves(MerkleNode node) {
        if (node == null) return 0;
        // Ritorna il conteggio delle foglie dal map, o 0 se non presente
        return leafCountMap.getOrDefault(node, 0);
    }

    /**
     * Trova l'indice di un hash all'interno di un branch specifico.
     *
     * @param node       il nodo corrente.
     * @param targetHash l'hash target da cercare.
     * @param offset     l'offset corrente.
     * @return l'indice se trovato; -1 altrimenti.
     */
    private int getIndexInBranch(MerkleNode node, String targetHash, int offset) {
        if (node == null) return -1;

        if (node.isLeaf()) {
            return node.getHash().equals(targetHash) ? offset : -1;
        }

        // Ricerca nel sottoalbero sinistro
        int leftLeafCount = getNumberOfLeaves(node.getLeft());
        int leftResult = getIndexInBranch(node.getLeft(), targetHash, offset);
        if (leftResult != -1) return leftResult;

        // Ricerca nel sottoalbero destro
        return getIndexInBranch(node.getRight(), targetHash, offset + leftLeafCount);
    }

    /**
     * Verifica se un branch è valido, ovvero se il suo hash è presente nell'albero.
     *
     * @param node1 nodo dell'albero corrente.
     * @param node2 nodo dell'altro albero.
     * @param start indice di inizio.
     * @param end   indice di fine.
     * @param invalidIndices insieme di indici invalidi.
     */
    private void getInvalidIndices(MerkleNode node1, MerkleNode node2,
                                   int start, int end, Set<Integer> invalidIndices) {
        if (node1 == null || node2 == null || start > end) return;

        // Controllo diretto dell'hash
        if (Objects.equals(node1.getHash(), node2.getHash())) return;

        // Caso foglia
        if (node1.isLeaf()) {
            invalidIndices.add(start);
            return;
        }

        // Ottiene il numero di foglie nel sottoalbero sinistro
        int leftLeafCount = getNumberOfLeaves(node1.getLeft());
        int centralPoint = start + leftLeafCount - 1;

        // Ricorsione nel sottoalbero sinistro
        getInvalidIndices(node1.getLeft(), node2.getLeft(),
                start, centralPoint, invalidIndices);

        // Ricorsione nel sottoalbero destro
        getInvalidIndices(node1.getRight(), node2.getRight(),
                centralPoint + 1, end, invalidIndices);
    }

    /**
     * Costruisce la prova di Merkle per un dato elemento.
     *
     * @param node       il nodo corrente.
     * @param targetHash l'hash target.
     * @param proof      l'oggetto MerkleProof da aggiornare.
     * @return true se il dato è stato trovato; false altrimenti.
     */
    private boolean generateProofForData(MerkleNode node, String targetHash, MerkleProof proof) {
        if (node == null) return false;

        if (node.isLeaf()) {
            if (node.getHash().equals(targetHash)) {
                return true;
            }
            return false;
        }

        // Se il target è nel sottoalbero sinistro
        if (generateProofForData(node.getLeft(), targetHash, proof)) {
            // Aggiungi l'hash del fratello destro, se non c'è aggiungi ""
            if (node.getRight() != null)
                proof.addHash(node.getRight().getHash(), false);
            else
                proof.addHash("", false);
            return true;
        }

        // Se il target è nel sottoalbero destro
        if (generateProofForData(node.getRight(), targetHash, proof)) {
            // Aggiungi l'hash del fratello sinistro, se non c'è aggiungi ""
            if (node.getLeft() != null)
                proof.addHash(node.getLeft().getHash(), true);
            else
                proof.addHash("", true);
            return true;
        }
        // Se il dato non è stato trovato in nessuno dei sottoalberi
        return false;
    }

    /**
     * Costruisce la prova di Merkle per un dato branch.
     *
     * @param node       il nodo corrente.
     * @param targetHash l'hash target.
     * @param proof      l'oggetto MerkleProof da aggiornare.
     * @return true se il branch è stato trovato; false altrimenti.
     */
    private boolean generateProofForBranch(MerkleNode node, String targetHash, MerkleProof proof) {
        if (node == null) return false;
        if (node.getHash().equals(targetHash)) return true;

        // Se il targetHash è nel sottoalbero sinistro
        if (generateProofForBranch(node.getLeft(), targetHash, proof)) {
            // Aggiungi l'hash del fratello destro se esiste, altrimenti ""
            if (node.getRight() != null)
                proof.addHash(node.getRight().getHash(), false);
            else
                proof.addHash("", false);
            return true;
        }

        // Se il targetHash è nel sottoalbero destro
        if (generateProofForBranch(node.getRight(), targetHash, proof)) {
            // Aggiungi l'hash del fratello sinistro se esiste, altrimenti ""
            if (node.getLeft() != null)
                proof.addHash(node.getLeft().getHash(), true);
            else
                proof.addHash("", true);
            return true;
        }
        // Se il branch non è stato trovato in nessuno dei sottoalberi
        return false;
    }

    /**
     * Trova l'indice di un branch specifico all'interno dell'albero.
     *
     * @param node       il nodo corrente.
     * @param referenceHash l'hash target.
     * @param offset     l'offset corrente.
     * @return l'indice se trovato; -1 altrimenti.
     */
    private int findBranchIndex(MerkleNode node, String referenceHash, int offset) {
        if (node == null) return -1;

        // Se l'hash del nodo corrente corrisponde al referente
        if (node.getHash().equals(referenceHash))
            // Se è un branch interno, non è direttamente associato a un indice foglia
            return -1;

        // Se il nodo è una foglia, verifica se l'hash corrisponde
        if (node.isLeaf())
            return node.getHash().equals(referenceHash) ? offset : -1;

        // Ricerca nel sottoalbero sinistro
        int leftLeafCount = getNumberOfLeaves(node.getLeft());
        int leftResult = findBranchIndex(node.getLeft(), referenceHash, offset);
        if (leftResult != -1) return leftResult;

        // Ricerca nel sottoalbero destro
        return findBranchIndex(node.getRight(), referenceHash, offset + leftLeafCount);
    }

}