/*
 * LZString4Java By Rufus Huang 
 * https://github.com/rufushuang/lz-string4java
 * MIT License
 * 
 * Port from original JavaScript version by pieroxy 
 * https://github.com/pieroxy/lz-string
 */

package blazing.chain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class LZSEncoding {

    private static final char[] keyStrBase64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=".toCharArray(),
            keyStrUriSafe = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+-$".toCharArray(),
            valStrBase64 = new char[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    62, 0, 0, 0, 63, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 0, 0, 0, 64, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,
                    0, 0, 0, 0, 0, 0, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51},
            valStrUriSafe = new char[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 64, 0, 0, 0, 0, 0, 0, 62, 0, 63, 0, 0,
                    52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,
                    0, 0, 0, 0, 0, 0, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51};

    public static String compressToBase64(String uncompressed) {
        if (uncompressed == null)
            return "";
        String res = LZSEncoding._compress(uncompressed, 6, keyStrBase64, 0);
        switch (res.length() & 3) { // To produce valid Base64
            default: // When could this happen ?
            case 0:
                return res;
            case 1:
                return res + "===";
            case 2:
                return res + "==";
            case 3:
                return res + "=";
        }
    }

    public static String decompressFromBase64(String compressed) {
        if (compressed == null)
            return null;
        if (compressed.isEmpty())
            return "";
        final char[] input = compressed.toCharArray();
        // function(index) { return getBaseValue(keyStrBase64,
        // input.charAt(index)); }
        return LZSEncoding._decompress(input.length, 32, input, valStrBase64, 0);
    }

    public static String compressToUTF16(String uncompressed) {
        if (uncompressed == null)
            return null;
        return LZSEncoding._compress(uncompressed, 15, null, 32) + " ";
    }

    public static String decompressFromUTF16(String compressed) {
        if (compressed == null)
            return null;
        if (compressed.isEmpty())
            return "";
        final char[] comp = compressed.toCharArray();
        return LZSEncoding._decompress(comp.length, 16384, comp, null, -32);
    }

    public static String compressToEncodedURIComponent(String uncompressed) {
        if (uncompressed == null)
            return null;
        return LZSEncoding._compress(uncompressed, 6, keyStrUriSafe, 0) + " ";
    }

    public static String decompressFromEncodedURIComponent(String compressed) {
        if (compressed == null) return null;
        if (compressed.isEmpty()) return "";
        compressed = compressed.replace(' ', '+');
        final char[] input = compressed.toCharArray();
        return LZSEncoding._decompress(input.length, 32, input, valStrUriSafe, 0);
    }

    public static String compress(String uncompressed) {
        return LZSEncoding._compress(uncompressed, 16, null, 0);
    }

    private static String _compress(String uncompressedStr, int bitsPerChar, char[] getCharFromInt, int offset) {
        if (uncompressedStr == null) return null;
        if (uncompressedStr.isEmpty()) return "";
        int i, value;
        HashMap<String, Integer> context_dictionary = new HashMap<String, Integer>();
        HashSet<String> context_dictionaryToCreate = new HashSet<String>();
        String context_c;
        String context_wc;
        String context_w = "";
        int context_enlargeIn = 2; // Compensate for the first entry which should not count
        int context_dictSize = 3;
        int context_numBits = 2;
        StringBuilder context_data = new StringBuilder(uncompressedStr.length() >>> 1);
        int context_data_val = 0;
        int context_data_position = 0;
        int ii;

        char[] uncompressed = uncompressedStr.toCharArray();
        for (ii = 0; ii < uncompressed.length; ii++) {
            context_c = String.valueOf(uncompressed[ii]);
            if (!context_dictionary.containsKey(context_c)) {
                context_dictionary.put(context_c, context_dictSize++);
                context_dictionaryToCreate.add(context_c);
            }

            context_wc = context_w + context_c;
            if (context_dictionary.containsKey(context_wc)) {
                context_w = context_wc;
            } else {
                if (context_dictionaryToCreate.contains(context_w)) {
                    if ((value = context_w.charAt(0)) < 256) {
                        for (i = 0; i < context_numBits; i++) {
                            context_data_val = (context_data_val << 1);
                            if (context_data_position == bitsPerChar - 1) {
                                context_data_position = 0;
                                context_data.append((char) (((getCharFromInt == null) ? context_data_val : getCharFromInt[context_data_val]) + offset));
                                context_data_val = 0;
                            } else {
                                context_data_position++;
                            }
                        }
                        for (i = 0; i < 8; i++) {
                            context_data_val = (context_data_val << 1) | (value & 1);
                            if (context_data_position == bitsPerChar - 1) {
                                context_data_position = 0;
                                context_data.append((char) (((getCharFromInt == null) ? context_data_val : getCharFromInt[context_data_val]) + offset));
                                context_data_val = 0;
                            } else {
                                context_data_position++;
                            }
                            value >>= 1;
                        }
                    } else {
                        value = 1;
                        for (i = 0; i < context_numBits; i++) {
                            context_data_val = (context_data_val << 1) | value;
                            if (context_data_position == bitsPerChar - 1) {
                                context_data_position = 0;
                                context_data.append((char) (((getCharFromInt == null) ? context_data_val : getCharFromInt[context_data_val]) + offset));
                                context_data_val = 0;
                            } else {
                                context_data_position++;
                            }
                            value = 0;
                        }
                        value = context_w.charAt(0);
                        for (i = 0; i < 16; i++) {
                            context_data_val = (context_data_val << 1) | (value & 1);
                            if (context_data_position == bitsPerChar - 1) {
                                context_data_position = 0;
                                context_data.append((char) (((getCharFromInt == null) ? context_data_val : getCharFromInt[context_data_val]) + offset));
                                context_data_val = 0;
                            } else {
                                context_data_position++;
                            }
                            value >>= 1;
                        }
                    }
                    context_enlargeIn--;
                    if (context_enlargeIn == 0) {
                        context_enlargeIn = 1 << context_numBits++;
                    }
                    context_dictionaryToCreate.remove(context_w);
                } else {
                    value = context_dictionary.get(context_w);
                    for (i = 0; i < context_numBits; i++) {
                        context_data_val = (context_data_val << 1) | (value & 1);
                        if (context_data_position == bitsPerChar - 1) {
                            context_data_position = 0;
                            context_data.append((char) (((getCharFromInt == null) ? context_data_val : getCharFromInt[context_data_val]) + offset));
                            context_data_val = 0;
                        } else {
                            context_data_position++;
                        }
                        value >>= 1;
                    }

                }
                context_enlargeIn--;
                if (context_enlargeIn == 0) {
                    context_enlargeIn = 1 << context_numBits++;
                }
                // Add wc to the dictionary.
                context_dictionary.put(context_wc, context_dictSize++);
                context_w = context_c;
            }
        }

        // Output the code for w.
        if (!context_w.isEmpty()) {
            if (context_dictionaryToCreate.contains(context_w)) {
                if (context_w.charAt(0) < 256) {
                    for (i = 0; i < context_numBits; i++) {
                        context_data_val = (context_data_val << 1);
                        if (context_data_position == bitsPerChar - 1) {
                            context_data_position = 0;
                            context_data.append((char) (((getCharFromInt == null) ? context_data_val : getCharFromInt[context_data_val]) + offset));
                            context_data_val = 0;
                        } else {
                            context_data_position++;
                        }
                    }
                    value = context_w.charAt(0);
                    for (i = 0; i < 8; i++) {
                        context_data_val = (context_data_val << 1) | (value & 1);
                        if (context_data_position == bitsPerChar - 1) {
                            context_data_position = 0;
                            context_data.append((char) (((getCharFromInt == null) ? context_data_val : getCharFromInt[context_data_val]) + offset));
                            context_data_val = 0;
                        } else {
                            context_data_position++;
                        }
                        value >>= 1;
                    }
                } else {
                    value = 1;
                    for (i = 0; i < context_numBits; i++) {
                        context_data_val = (context_data_val << 1) | value;
                        if (context_data_position == bitsPerChar - 1) {
                            context_data_position = 0;
                            context_data.append((char) (((getCharFromInt == null) ? context_data_val : getCharFromInt[context_data_val]) + offset));
                            context_data_val = 0;
                        } else {
                            context_data_position++;
                        }
                        value = 0;
                    }
                    value = context_w.charAt(0);
                    for (i = 0; i < 16; i++) {
                        context_data_val = (context_data_val << 1) | (value & 1);
                        if (context_data_position == bitsPerChar - 1) {
                            context_data_position = 0;
                            context_data.append((char) (((getCharFromInt == null) ? context_data_val : getCharFromInt[context_data_val]) + offset));
                            context_data_val = 0;
                        } else {
                            context_data_position++;
                        }
                        value >>= 1;
                    }
                }

                context_dictionaryToCreate.remove(context_w);
            } else {
                value = context_dictionary.get(context_w);
                for (i = 0; i < context_numBits; i++) {
                    context_data_val = (context_data_val << 1) | (value & 1);
                    if (context_data_position == bitsPerChar - 1) {
                        context_data_position = 0;
                        context_data.append((char) (((getCharFromInt == null) ? context_data_val : getCharFromInt[context_data_val]) + offset));
                        context_data_val = 0;
                    } else {
                        context_data_position++;
                    }
                    value >>= 1;
                }

            }
        }

        // Mark the end of the stream
        value = 2;
        for (i = 0; i < context_numBits; i++) {
            context_data_val = (context_data_val << 1) | (value & 1);
            if (context_data_position == bitsPerChar - 1) {
                context_data_position = 0;
                context_data.append((char) (((getCharFromInt == null) ? context_data_val : getCharFromInt[context_data_val]) + offset));
                context_data_val = 0;
            } else {
                context_data_position++;
            }
            value >>= 1;
        }

        // Flush the last char
        while (true) {
            context_data_val = (context_data_val << 1);
            if (context_data_position == bitsPerChar - 1) {
                context_data.append((char) (((getCharFromInt == null) ? context_data_val : getCharFromInt[context_data_val]) + offset));
                break;
            } else
                context_data_position++;
        }
        return context_data.toString();
    }

    public static String decompress(final String compressed) {
        if (compressed == null)
            return null;
        if (compressed.isEmpty())
            return "";
        return LZSEncoding._decompress(compressed.length(), 32768, compressed.toCharArray(), null, 0);
    }

    private static String _decompress(int length, int resetValue, char[] getNextValue, char[] modify, int offset) {
        ArrayList<String> dictionary = new ArrayList<String>();
        int enlargeIn = 4, dictSize = 4, numBits = 3, position = resetValue, index = 1, resb, maxpower, power;
        String entry, w, c;
        ArrayList<String> result = new ArrayList<String>();
        char bits, val = (modify == null) ? (char) (getNextValue[0] + offset) : modify[getNextValue[0]];

        for (char i = 0; i < 3; i++) {
            dictionary.add(i, String.valueOf(i));
        }

        bits = 0;
        maxpower = 2;
        power = 0;
        while (power != maxpower) {
            resb = val & position;
            position >>= 1;
            if (position == 0) {
                position = resetValue;
                val = (modify == null) ? (char) (getNextValue[index++] + offset) : modify[getNextValue[index++]];
            }
            bits |= (resb > 0 ? 1 : 0) << power++;
        }

        switch (bits) {
            case 0:
                bits = 0;
                maxpower = 8;
                power = 0;
                while (power != maxpower) {
                    resb = val & position;
                    position >>= 1;
                    if (position == 0) {
                        position = resetValue;
                        val = (modify == null) ? (char) (getNextValue[index++] + offset) : modify[getNextValue[index++]];
                    }
                    bits |= (resb > 0 ? 1 : 0) << power++;
                }
                c = String.valueOf(bits);
                break;
            case 1:
                bits = 0;
                maxpower = 16;
                power = 0;
                while (power != maxpower) {
                    resb = val & position;
                    position >>= 1;
                    if (position == 0) {
                        position = resetValue;
                        val = (modify == null) ? (char) (getNextValue[index++] + offset) : modify[getNextValue[index++]];
                    }
                    bits |= (resb > 0 ? 1 : 0) << power++;
                }
                c = String.valueOf(bits);
                break;
            default:
                return "";
        }
        dictionary.add(c);
        w = c;
        result.add(w);
        while (true) {
            if (index > length) {
                return "";
            }

            bits = 0;
            maxpower = numBits;
            power = 0;
            while (power != maxpower) {
                resb = val & position;
                position >>= 1;
                if (position == 0) {
                    position = resetValue;
                    val = (modify == null) ? (char) (getNextValue[index++] + offset) : modify[getNextValue[index++]];
                }
                bits |= (resb > 0 ? 1 : 0) << power++;
            }
            int cc;
            switch (cc = bits) {
                case 0:
                    bits = 0;
                    maxpower = 8;
                    power = 0;
                    while (power != maxpower) {
                        resb = val & position;
                        position >>= 1;
                        if (position == 0) {
                            position = resetValue;
                            val = (modify == null) ? (char) (getNextValue[index++] + offset) : modify[getNextValue[index++]];
                        }
                        bits |= (resb > 0 ? 1 : 0) << power++;
                    }

                    dictionary.add(String.valueOf(bits));
                    cc = dictSize++;
                    enlargeIn--;
                    break;
                case 1:
                    bits = 0;
                    maxpower = 16;
                    power = 0;
                    while (power != maxpower) {
                        resb = val & position;
                        position >>= 1;
                        if (position == 0) {
                            position = resetValue;
                            val = (modify == null) ? (char) (getNextValue[index++] + offset) : modify[getNextValue[index++]];
                        }
                        bits |= (resb > 0 ? 1 : 0) << power++;
                    }
                    dictionary.add(String.valueOf(bits));
                    cc = dictSize++;
                    enlargeIn--;
                    break;
                case 2:
                    StringBuilder sb = new StringBuilder(result.size());
                    for (String s : result)
                        sb.append(s);
                    return sb.toString();
            }

            if (enlargeIn == 0) {
                enlargeIn = 1 << numBits;
                numBits++;
            }

            if (cc < dictionary.size() && dictionary.get(cc) != null) {
                entry = dictionary.get(cc);
            } else {
                if (cc == dictSize) {
                    entry = w + w.charAt(0);
                } else {
                    return "";
                }
            }
            result.add(entry);

            // Add w+entry[0] to the dictionary.
            dictionary.add(w + entry.charAt(0));
            dictSize++;
            enlargeIn--;

            w = entry;

            if (enlargeIn == 0) {
                enlargeIn = 1 << numBits;
                numBits++;
            }

        }

    }
}
