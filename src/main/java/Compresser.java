import java.io.*;
import java.util.HashMap;

public class Compresser {
    private final int ASCII_COUNT = 256;
    private final int BIT_SIZE = 13;
    private final int MAX_SIZE = 8192;

    public String compress(String input) throws IOException {
        //Имя выходного файла
        String output = FileRenamer.getNameCompressedFile(input, ".lzw");
        //Читалка и записывалка
        DataInputStream read = new DataInputStream(new BufferedInputStream(new FileInputStream(input)));
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(output)));

        //Мапа(словарь)
        HashMap<String, Integer> vocabulary = new HashMap<String, Integer>();
        //Заполняем мапу
        for(int i = 0; i < ASCII_COUNT; i++) {
            vocabulary.put(Character.toString((char) i), i);
        }

        byte readByte; //Считанный байт
        StringBuilder temp = new StringBuilder(); //Буфер символов
        StringBuilder bitTemp = new StringBuilder(); //Буфер битов (которые потом в байты)
        int i; //Вспомагалка
        char c; //1 символ для буфера
        int count = ASCII_COUNT; // Следуюий номер сиволов  в таблце


        //Ne obyazatelno
        readByte = read.readByte();
        i = new Byte(readByte).intValue();
        if (i < 0) {
            i += ASCII_COUNT;
        }
        c = (char) i;
        temp.append(c);
        //Ne

        while (true) {
            try {
                readByte = read.readByte();
            } catch (EOFException e) {
                break;
            }

            i = new Byte(readByte).intValue();
            if (i < 0) {
                i += ASCII_COUNT;
            }
            c = (char) i; // Байты в символ (из файла)

            if (vocabulary.containsKey(temp.toString() + c)) { // Если в словаре есть последовательность
                temp.append(c); // Клеим символ к буферу
            } else {
                //Тут клеим к битТемпу вот что: берем из мапы значение по ключу темпа. и переводим его в 12 бит.
                String bits = toBits(vocabulary.get(temp.toString()));
                bitTemp.append(bits);

                while (bitTemp.length() >= 8) {
                    out.writeByte((byte) Integer.parseInt(bitTemp.substring(0, 8), 2));
                    bitTemp.delete(0, 8);
                }

                if(count < MAX_SIZE) { //Не превышаем макса
                    temp.append(c);
                    vocabulary.put(temp.toString(), count++);
                }

                temp = new StringBuilder((String.valueOf(c))); //Чистим буфер, оставляем последний
            }
        }

        bitTemp.append(toBits(vocabulary.get(temp.toString())));
        while (bitTemp.length() > 0) {
            while (bitTemp.length() < 8) {
                bitTemp.append('0');
            }
            out.writeByte((byte) Integer.parseInt(bitTemp.substring(0, 8), 2));
            bitTemp.delete(0, 8);
        }

        read.close();
        out.close();

        return output;
    }

    private String toBits(int i) {
        StringBuilder temp = new StringBuilder(Integer.toBinaryString(i));
        while (temp.length() < BIT_SIZE) {
            temp.insert(0, "0");
        }
        return temp.toString();
    }

    //Раскодируем
    public void decompress(String input) throws IOException {
        int charArraySize = MAX_SIZE;
        String[] charArray = new String[charArraySize];
        for (int i = 0; i < ASCII_COUNT; i++) {
            charArray[i] = Character.toString((char) i);
        }
        int count = ASCII_COUNT;

        String output = FileRenamer.getNameDecompressedFile(input, ".lzw");

        try (
                DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(input)));
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(output)))
        ) {

            int currentWord, priorityWord;
            StringBuilder bitTemp = new StringBuilder();
            readNext(bitTemp, in);
            readNext(bitTemp, in);

            priorityWord = getValue(bitTemp);

            out.writeBytes(charArray[priorityWord]);

            boolean isNotEnd = true;

            while (isNotEnd) {
                try {
                    while (bitTemp.length() < BIT_SIZE) {
                        readNext(bitTemp, in);
                    }
                } catch (EOFException e) {
                    isNotEnd = false;
                }
                if(bitTemp.length() < BIT_SIZE) {
                    break;
                } else {
                    currentWord = getValue(bitTemp);
                }

                if (currentWord >= count) {
                    String s = charArray[priorityWord] + charArray[priorityWord].charAt(0);
                    if (count < charArraySize) {
                        charArray[count] = s;
                    }
                    count++;
                    out.writeBytes(s);
                } else {
                    if (count < charArraySize) {
                        charArray[count] = charArray[priorityWord] + charArray[currentWord].charAt(0);
                    }
                    count++;
                    out.writeBytes(charArray[currentWord]);
                }
                priorityWord = currentWord;
            }

            in.close();
            out.close();
        }

    }

    private int getValue(StringBuilder bitTemp) {
        int i = Integer.parseInt(bitTemp.substring(0, BIT_SIZE), 2);
        bitTemp.delete(0, BIT_SIZE);
        return i;
    }

    private void readNext(StringBuilder bitTemp, DataInputStream in) throws IOException {
        bitTemp.append(byteToBits(in.readByte(), 8));
    }

    private String byteToBits(byte b, int size) {
        return String.format("%" + size + "s", Integer.toBinaryString(b & 0xFF))
                .replace(' ', '0');
    }

}
