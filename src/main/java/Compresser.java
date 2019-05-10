import java.io.*;
import java.util.HashMap;

public class Compresser {
    private final int ASCII_COUNT = 256;
    private final int BIT_SIZE = 13;
    private final int MAX_SIZE = 8192;

    public String compress(String input) throws IOException {
        //��� ��������� �����
        String output = FileRenamer.getNameCompressedFile(input, ".lzw");
        //������� � �����������
        DataInputStream read = new DataInputStream(new BufferedInputStream(new FileInputStream(input)));
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(output)));

        //����(�������)
        HashMap<String, Integer> vocabulary = new HashMap<String, Integer>();
        //��������� ����
        for(int i = 0; i < ASCII_COUNT; i++) {
            vocabulary.put(Character.toString((char) i), i);
        }

        byte readByte; //��������� ����
        StringBuilder temp = new StringBuilder(); //����� ��������
        StringBuilder bitTemp = new StringBuilder(); //����� ����� (������� ����� � �����)
        int i; //�����������
        char c; //1 ������ ��� ������
        int count = ASCII_COUNT; // �������� ����� �������  � ������


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
            c = (char) i; // ����� � ������ (�� �����)

            if (vocabulary.containsKey(temp.toString() + c)) { // ���� � ������� ���� ������������������
                temp.append(c); // ����� ������ � ������
            } else {
                //��� ����� � �������� ��� ���: ����� �� ���� �������� �� ����� �����. � ��������� ��� � 12 ���.
                String bits = toBits(vocabulary.get(temp.toString()));
                bitTemp.append(bits);

                while (bitTemp.length() >= 8) {
                    out.writeByte((byte) Integer.parseInt(bitTemp.substring(0, 8), 2));
                    bitTemp.delete(0, 8);
                }

                if(count < MAX_SIZE) { //�� ��������� �����
                    temp.append(c);
                    vocabulary.put(temp.toString(), count++);
                }

                temp = new StringBuilder((String.valueOf(c))); //������ �����, ��������� ���������
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

    //�����������
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
