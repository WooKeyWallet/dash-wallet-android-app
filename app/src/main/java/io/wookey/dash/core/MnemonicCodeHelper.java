package io.wookey.dash.core;

import android.text.TextUtils;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.crypto.MnemonicException;

import java.util.Collections;
import java.util.List;

public class MnemonicCodeHelper {
    /**
     * Convert mnemonic word list to original entropy value.
     */
    public static byte[] toEntropy(List<String> words, List<String> wordList) throws MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException, MnemonicException.MnemonicChecksumException {
        if (words.size() % 3 > 0)
            throw new MnemonicException.MnemonicLengthException("Word list size must be multiple of three words.");

        if (words.size() == 0)
            throw new MnemonicException.MnemonicLengthException("Word list is empty.");

        // Look up all the words in the list and construct the
        // concatenation of the original entropy and the checksum.
        //
        int concatLenBits = words.size() * 11;
        boolean[] concatBits = new boolean[concatLenBits];
        int wordindex = 0;
        for (String word : words) {
            // Find the words index in the wordlist.
            int ndx = search(wordList, word);
            if (ndx < 0)
                throw new MnemonicException.MnemonicWordException(word);

            // Set the next 11 bits to the value of the index.
            for (int ii = 0; ii < 11; ++ii)
                concatBits[(wordindex * 11) + ii] = (ndx & (1 << (10 - ii))) != 0;
            ++wordindex;
        }

        int checksumLengthBits = concatLenBits / 33;
        int entropyLengthBits = concatLenBits - checksumLengthBits;

        // Extract original entropy as bytes.
        byte[] entropy = new byte[entropyLengthBits / 8];
        for (int ii = 0; ii < entropy.length; ++ii)
            for (int jj = 0; jj < 8; ++jj)
                if (concatBits[(ii * 8) + jj])
                    entropy[ii] |= 1 << (7 - jj);

        // Take the digest of the entropy.
        byte[] hash = Sha256Hash.hash(entropy);
        boolean[] hashBits = bytesToBits(hash);

        // Check all the checksum bits.
        for (int i = 0; i < checksumLengthBits; ++i)
            if (concatBits[entropyLengthBits + i] != hashBits[i])
                throw new MnemonicException.MnemonicChecksumException();

        return entropy;
    }

    public static int search(List<String> wordList, String key) {
        for (int i = 0, len = wordList.size(); i < len; i++) {
            if (TextUtils.equals(wordList.get(i), key)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Check to see if a mnemonic word list is valid.
     */
    public static void check(List<String> words, List<String> wordList) throws MnemonicException {
        toEntropy(words, wordList);
    }

    private static boolean[] bytesToBits(byte[] data) {
        boolean[] bits = new boolean[data.length * 8];
        for (int i = 0; i < data.length; ++i)
            for (int j = 0; j < 8; ++j)
                bits[(i * 8) + j] = (data[i] & (1 << (7 - j))) != 0;
        return bits;
    }
}
