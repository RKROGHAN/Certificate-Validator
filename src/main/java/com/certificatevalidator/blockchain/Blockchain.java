package com.certificatevalidator.blockchain;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the blockchain that maintains certificate integrity.
 * Each block contains a certificate hash and links to the previous block.
 */
public class Blockchain {
    private final List<Block> chain;
    private final String genesisHash;

    /**
     * Constructor initializes the blockchain with a genesis block
     */
    public Blockchain() {
        this.chain = new ArrayList<>();
        this.genesisHash = "0";
        createGenesisBlock();
    }

    /**
     * Creates the first block (genesis block) in the blockchain
     */
    private void createGenesisBlock() {
        Block genesisBlock = new Block(0, "genesis", genesisHash);
        chain.add(genesisBlock);
    }

    /**
     * Adds a new block to the blockchain
     * @param certificateHash Hash of the certificate to be added
     * @return The newly created block
     */
    public Block addBlock(String certificateHash) {
        String previousHash = getLatestBlock().getCurrentHash();
        int newIndex = chain.size();
        
        Block newBlock = new Block(newIndex, certificateHash, previousHash);
        chain.add(newBlock);
        return newBlock;
    }

    /**
     * Adds an existing block to the blockchain (for loading from database)
     * @param block The block to add
     */
    public void addExistingBlock(Block block) {
        chain.add(block);
    }

    /**
     * Gets the latest block in the chain
     * @return The most recent block
     */
    public Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }

    /**
     * Gets all blocks in the blockchain
     * @return List of all blocks
     */
    public List<Block> getChain() {
        return new ArrayList<>(chain);
    }

    /**
     * Validates the integrity of the entire blockchain
     * @return true if the chain is valid, false otherwise
     */
    public boolean isChainValid() {
        for (int i = 1; i < chain.size(); i++) {
            Block currentBlock = chain.get(i);
            Block previousBlock = chain.get(i - 1);

            // Check if current block's hash is valid
            if (!currentBlock.isValid()) {
                System.out.println("Block " + i + " has invalid hash");
                return false;
            }

            // Check if current block's previous hash matches previous block's hash
            if (!currentBlock.getPreviousHash().equals(previousBlock.getCurrentHash())) {
                System.out.println("Block " + i + " has invalid previous hash");
                return false;
            }
        }
        return true;
    }

    /**
     * Searches for a certificate hash in the blockchain
     * @param certificateHash The hash to search for
     * @return true if the certificate exists in the blockchain
     */
    public boolean containsCertificate(String certificateHash) {
        return chain.stream()
                .anyMatch(block -> block.getCertificateHash().equals(certificateHash));
    }

    /**
     * Gets the block containing a specific certificate hash
     * @param certificateHash The certificate hash to find
     * @return The block containing the certificate, or null if not found
     */
    public Block getBlockByCertificateHash(String certificateHash) {
        return chain.stream()
                .filter(block -> block.getCertificateHash().equals(certificateHash))
                .findFirst()
                .orElse(null);
    }

    /**
     * Removes a block containing a specific certificate hash
     * Note: This will break chain integrity, but allows certificate deletion
     * @param certificateHash The certificate hash to remove
     * @return true if a block was removed, false otherwise
     */
    public boolean removeBlockByCertificateHash(String certificateHash) {
        return chain.removeIf(block -> 
            block.getIndex() > 0 && block.getCertificateHash().equals(certificateHash)
        );
    }
    
    /**
     * Validates chain integrity only for blocks that exist
     * This is more lenient than isChainValid() and allows for deleted blocks
     * @return true if existing blocks form a valid chain
     */
    public boolean isChainValidLenient() {
        if (chain.size() <= 1) {
            return true; // Only genesis block or empty
        }
        
        // Check each block against its previous block
        for (int i = 1; i < chain.size(); i++) {
            Block currentBlock = chain.get(i);
            Block previousBlock = chain.get(i - 1);
            
            // Check if current block's hash is valid
            if (!currentBlock.isValid()) {
                return false;
            }
            
            // Check if current block's previous hash matches previous block's hash
            // But only if previous block is not genesis (genesis has special handling)
            if (previousBlock.getIndex() > 0 && !currentBlock.getPreviousHash().equals(previousBlock.getCurrentHash())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the total number of blocks in the chain
     * @return Number of blocks
     */
    public int getChainLength() {
        return chain.size();
    }

    /**
     * Clears all blocks except the genesis block
     */
    public void clear() {
        chain.clear();
        createGenesisBlock();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Blockchain{\n");
        for (Block block : chain) {
            sb.append("  ").append(block.toString()).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }
}
