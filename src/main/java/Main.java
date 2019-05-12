import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Archiver archiver = new Archiver();
        String fileName = "image.bmp";
        String archive = archiver.archive(fileName);
        archiver.unzip(fileName, archive);
    }
}
