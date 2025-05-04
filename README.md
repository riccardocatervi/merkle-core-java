# Merkle Core - Java 

Robust Java 8 implementation of Merkle trees, hash‑linked lists and inclusion proofs based on the MD5 message‑digest algorithm.  The code base is 100 % pure Java SE 8 with no external runtime dependencies, accompanied by a comprehensive JUnit test‑suite.

## Table of Contents
- [Motivation](#motivation)
- [Features](#features)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [API Overview](#api-overview)
- [Usage Examples](usage-examples)
- [Building & Testing](building-&-testing)
- [Extending the Library](extending-the-library)
- [Performance Notes](performance-notes)
- [Contributing](contributing)
- [Licence](licence)

## Motivation
A Merkle tree (a.k.a. *binary hash tree*) allows any consumer to verify the authenticity and integrity of a large data set by exchanging only *O(log n)* data instead of the whole collection.  The same principle underlies blockchains, peer‑to‑peer file verification (THEX), Amazon Dynamo anti‑entropy repair and many more.

This repository distils the core idea into a lightweight, self‑contained Java 8 library suitable for:

- academic assignments,
- teaching data‑structure fundamentals,
- embedding in JVM applications that require tamper‑evident storage.

## Features
- Generic Merkle tree supporting any payload type `T` (hash derived from `hashCode()` by default).
- Immutable nodes (`MerkleNode`) – thread‑safe by design.
- Hash‑linked list (`HashLinkedList`) that computes the MD5 digest for each element on insertion.
- Inclusion / consistency proofs (`MerkleProof`) with constant‑time verification once the proof is built.
- Utilities (`HashUtil`) for MD5 digest and byte conversions.
- 100 % unit‑test coverage (JUnit 5) – validating hash generation, tree building, proof correctness and negative scenarios.
- Zero third‑party dependencies – the library compiles with the stock `javac` shipped in JDK 8.

## Quick Start
```bash
# Clone the repository
git clone https://github.com/riccardocatervi/merkle-core-java
cd merkle-core-java

# Compile source & tests (plain javac)
javac -d out $(find src/main/java -name "*.java")
javac -cp lib/junit-4.13.2.jar:out -d out/test $(find src/test/java -name "*.java")

# Run a sample program
java -cp out it.unicam.cs.asdl2425.mp1.Sample
```

**Tip** Import the project as a *Plain Java* module in IntelliJ IDEA / Eclipse.  The directory layout follows the standard Maven convention (`src/main/java`, `src/test/java`) so your IDE will recognise the sources automatically.

## Project Structure
<pre><code>
merkle-core-java
├── src
│   ├── main
│   │   └── java
│   │       ├── HashLinkedList.java
│   │       ├── HashUtil.java
│   │       ├── MerkleNode.java
│   │       ├── MerkleProof.java
│   │       └── MerkleTree.java
│   └── test
│       └── java
│           ├── HashLinkedListTest.java
│           ├── HashUtilTest.java
│           ├── MerkleNodeTest.java
│           ├── MerkleProofTest.java
│           └── MerkleTreeTest.java
└── lib
    └── junit-4.13.2.jar  (test‑time only)
</code></pre>

## API Overview

| Class            | Responsibility |
|------------------|----------------|
| `HashUtil`       | Static helpers to compute MD5 digests from arbitrary data and from byte arrays. |
| `HashLinkedList<T>` | Singly‑linked list that stores payload *and* its MD5 hash at insertion time; offers O(1) head/tail insertions and `getAllHashes()` convenience. |
| `MerkleNode`     | Immutable node holding a hash and (optional) left/right child references; leaves and inner nodes are represented uniformly. |
| `MerkleTree<T>`  | Builds a balanced tree from a `HashLinkedList` of payload, exposes:<br>`validateData`, `validateBranch`, `validateTree`, `getMerkleProof` for leaves or arbitrary branches, `findInvalidDataIndices` to pinpoint corrupted leaves.<br>Accessor methods `getRoot()`, `getWidth()`, `getHeight()` are O(1). |
| `MerkleProof`    | Self‑contained list of sibling hashes (“audit path”). Includes:<br>`proveValidityOfData(Object)`, `proveValidityOfBranch(MerkleNode)` that recompute the root hash in O(k) where *k = log₂ n*. |

## Using Examples

### 1 – Building and Validating a Tree

```java
// Create a hash-linked list with some payload
HashLinkedList<String> list = new HashLinkedList<>();
list.addAtTail("Alice");
list.addAtTail("Bob");
list.addAtTail("Charlie");

// Build the Merkle tree
MerkleTree<String> tree = new MerkleTree<>(list);
System.out.println("Root hash = " + tree.getRoot().getHash());

// Validate that a datum belongs to the tree
boolean ok = tree.validateData("Bob");  // true
```
### 2 - Merkle Proof for a Leaf

```java
// Obtain the audit path for a specific leaf
MerkleProof proof = tree.getMerkleProof("Charlie");

// Transmit <payload, proof> to a remote verifier …
boolean verified = proof.proveValidityOfData("Charlie");
System.out.println("Proof valid? " + verified);  // true
```
### Detecting Corrupted Data

```java
// Obtain the audit path for a specific leaf
MerkleProof proof = tree.getMerkleProof("Charlie");

// Transmit <payload, proof> to a remote verifier …
boolean verified = proof.proveValidityOfData("Charlie");
System.out.println("Proof valid? " + verified);  // true
```
## Building & Testing
### Prerequisites
- **JDK 8** or later (the code does not rely on features introduced after Java 8).
- `bash`, `make` or any modern IDE if you prefer point‑and‑click.
### Command‑line build (Unix‑like)
A minimal Makefile is provided in *extras/Makefile* (optional).  Otherwise:
```java
mkdir -p out
javac -d out $(find src/main/java -name "*.java")
# compile & run tests
javac -cp lib/junit-4.13.2.jar:out -d out/test $(find src/test/java -name "*.java")
java  -cp lib/junit-4.13.2.jar:lib/hamcrest-core-1.3.jar:out org.junit.runner.JUnitCore it.unicam.cs.asdl2425.mp1.MerkleTreeTest
```
### IDE import
1. File -> New -> Project from Existing Sources.
2. Choose *Java* and point to the repository root.
3. Mark `src/main/java` as *Sources* and `src/test/java` as *Test Sources*.
4. Add `lib/junit-platform-console-standalone-1.12.0.jar` to the classpath.

## Extending the Library
- **Alternative hash functions** – subclass `HashUtil` or inject your own digest calculator, then replace calls to `HashUtil.computeMD5(...)` / `dataToHash(...)`.
- **Serialisation** – `MerkleNode` is immutable; you can add Jackson / Gson adapters without touching core logic.
- **Streaming construction** – for very large data sets, implement a builder that feeds blocks incrementally rather than materialising all leaves in memory.

## Performance Notes

| Operation                 | Time                                | Space                         |
|---------------------------|-------------------------------------|-------------------------------|
| Tree construction         | *O(n)*                              | *O(n)* (stores every hash once) |
| Validate leaf / branch    | *O(1)*                              | –                             |
| Generate proof            | *O(log n)*                          | *O(log n)*                    |
| Proof verification        | *O(log n)*                          | *O(1)*                        |
| Locate corrupted leaves   | worst-case *O(k log n)* where *k* =<br>number of corrupted leaves | – |

## Contributing
Pull requests are welcome!  Please open an issue first to discuss major changes.  

## Licence

This project is distributed under the terms of the [MIT Licence](LICENSE).

The overall structure and project template were originally designed by **Professor Luca Tesei** from the **University of Camerino (UNICAM)**, as part of the course activities in Algorithms and Data Structures.

Special thanks to Professor Tesei for his excellent lectures and guidance, which laid the groundwork for the design and implementation of this project.



