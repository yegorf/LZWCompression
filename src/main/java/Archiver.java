import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Archiver {
    private final int BYTE_SIZE = 8;
    private final int ASCII_COUNT = 256;
    private final int BIT_SIZE = 13;
    private final int MAX_SIZE = 8192;

    public String archive(String fileName) throws IOException {
        String archive = fileName + ".lzw";
        DataInputStream inputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)));
        DataOutputStream outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(archive)));

        HashMap<String, Integer> vocabulary = new HashMap<>();
        for(int i = 0; i < ASCII_COUNT; i++) {
            vocabulary.put(Character.toString((char) i), i);
        }

        byte readByte;
        StringBuilder charBuffer = new StringBuilder();
        StringBuilder bitsBuffer = new StringBuilder();
        int count = ASCII_COUNT;
        char symbol;

        while (true) {
            try {
                readByte = inputStream.readByte();
            } catch (EOFException e) {
                break;
            }
            symbol = getChar(readByte);

            if (vocabulary.containsKey(charBuffer.toString() + symbol)) { // Если в словаре есть последовательность
                charBuffer.append(symbol);
            } else {
                //Тут клеим к битТемпу вот что: берем из мапы значение по ключу темпа. и переводим его в 12 бит.
                String bits = toBits(vocabulary.get(charBuffer.toString()));
                bitsBuffer.append(bits);

                if(count < MAX_SIZE) {
                    charBuffer.append(symbol);
                    vocabulary.put(charBuffer.toString(), count);
                    count++;
                }
                charBuffer = new StringBuilder((String.valueOf(symbol)));
            }
        }

        bitsBuffer.append(toBits(vocabulary.get(charBuffer.toString())));
        while (bitsBuffer.length() > 0) {
            while (bitsBuffer.length() < BYTE_SIZE) {
                bitsBuffer.append('0');
            }
            outputStream.writeByte((byte) Integer.parseInt(bitsBuffer.substring(0, 8), 2));
            bitsBuffer.delete(0, BYTE_SIZE);
        }

        inputStream.close();
        outputStream.close();

        return archive;
    }

    public void unzip(String fileName, String archive) throws IOException {
        String output = "new-" + fileName;
        DataInputStream inputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(archive)));
        DataOutputStream outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(output)));

        ArrayList<String> vocabulary = new ArrayList<>();
        for (int i = 0; i < ASCII_COUNT; i++) {
            vocabulary.add(Character.toString((char) i));
        }

        int currentWord;
        int priorityWord;
        boolean neof = true;

        StringBuilder bitsBuffer = new StringBuilder();
        read(bitsBuffer, inputStream);
        read(bitsBuffer, inputStream);

        priorityWord = getNum(bitsBuffer);

        outputStream.writeBytes(vocabulary.get(priorityWord));

        while (neof) {
            try {
                while (bitsBuffer.length() < BIT_SIZE) {
                    read(bitsBuffer, inputStream);
                }
            } catch (EOFException e) {
                neof = false;
            }
            if(bitsBuffer.length() < BIT_SIZE) {
                break;
            } else {
                currentWord = getNum(bitsBuffer);
            }

            if (currentWord >= vocabulary.size()) {
                String s = vocabulary.get(priorityWord) + vocabulary.get(priorityWord).charAt(0);
                if (vocabulary.size() < MAX_SIZE) {
                    vocabulary.add(s);
                }
                outputStream.writeBytes(s);
            } else {
                if (vocabulary.size() < MAX_SIZE) {
                    vocabulary.add(vocabulary.get(priorityWord) + vocabulary.get(currentWord).charAt(0));
                }
                outputStream.writeBytes(vocabulary.get(currentWord));
            }
            priorityWord = currentWord;
        }

        inputStream.close();
        outputStream.close();
    }

    private char getChar(Byte b) {
        int i = b.intValue();
        if (i < 0) {
            i += ASCII_COUNT;
        }
        return (char) i;
    }

    private String toBits(int i) {
        StringBuilder temp = new StringBuilder(Integer.toBinaryString(i));
        while (temp.length() < BIT_SIZE) {
            temp.insert(0, "0");
        }
        return temp.toString();
    }

    private String byteToBits(byte b) {
        return String.format("%" + BYTE_SIZE + "s", Integer.toBinaryString(b & 0xFF))
                .replace(' ', '0');
    }

    private int getNum(StringBuilder bitTemp) {
        int i = Integer.parseInt(bitTemp.substring(0, BIT_SIZE), 2);
        bitTemp.delete(0, BIT_SIZE);
        return i;
    }

    private void read(StringBuilder bitTemp, DataInputStream in) throws IOException {
        bitTemp.append(byteToBits(in.readByte()));
    }
}
