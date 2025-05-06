import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class StoreData {
    static String recorderVersion ="version 0.1";
    static String jsonString = null;
    static String pathPrefix = "C:\\Data\\er_path";
    static String filePrefix = "er_file";
    static int FS = 48000;
    static int MAX_FILES_PER_FOLDER = 100;
    static int fileCounter = 0;
    static int folderCounter = 0;

    void createFolder(String name){
        File filePath = new File(name);
        if (!filePath.exists()) {filePath.mkdir();}
    }
    public String getPathPrefix(){return pathPrefix;}
    public void setPathPrefix(String pathName){
        pathPrefix = pathName;
        createFolder(pathPrefix);
    }
    public String [] storeAudio(double[][] buffer, int bufferlength, int inp, int length,
                                int channels) throws IOException {
        LocalDateTime localTimeNow = LocalDateTime.now(ZoneId.systemDefault());
        LocalDateTime startTime = localTimeNow.minusSeconds(length/FS);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("_yyyy_MM_dd_H_mm_ss");
        String timestamp = startTime.format(dateTimeFormatter);
        String pathName = pathPrefix+"/"+String.valueOf(folderCounter);
        createFolder(pathName);

        // File name
        String fileName = pathName+"/"+filePrefix+timestamp+".wav";
        File out = new File(fileName);

        final boolean bigEndian = true;
        final boolean signed = true;
        final int bits = 16;
        final byte [] byteBuffer = new byte[2*length*channels];

        int bufferIndex = inp-length;
        if (bufferIndex<0){bufferIndex += bufferlength;}
        int i = 0;
        for (int c1 = 0; c1 < length; c1++) {
            for (int c2=0; c2<channels; c2++) {
                final int x = (int) (buffer[c2][bufferIndex]);
                byteBuffer[i++] = (byte)(x >>> 8);
                byteBuffer[i++] = (byte)x;
            }
            if (++bufferIndex==bufferlength){
                bufferIndex = 0;
            }
        }

        AudioFormat format = new AudioFormat((float)FS, bits, channels, signed, bigEndian);
        ByteArrayInputStream bais = new ByteArrayInputStream(byteBuffer);
        AudioInputStream audioInputStream = new AudioInputStream(bais, format, length);
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, out);
        audioInputStream.close();

        // Manage folder splitting
        if (++fileCounter == MAX_FILES_PER_FOLDER){
            fileCounter = 0;
            folderCounter++;
        }
        return new String [] {fileName,timestamp};
    }
}
