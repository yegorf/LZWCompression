import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Compresser compresser = new Compresser();
        String file = compresser.compress("image.bmp");
        compresser.decompress(file);
    }
}
