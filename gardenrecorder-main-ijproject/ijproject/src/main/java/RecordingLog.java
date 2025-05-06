import java.io.FileWriter;
import java.io.IOException;

public class RecordingLog {
    static FileWriter jsonWriter = null;
    static String recorderVersion = "audio recorder v.0.1";
    static String jsonString = "{\"recorder\":"+recorderVersion+"\",";

    public void initializeJsonWriter(String path, String name) throws IOException {
       jsonWriter = new FileWriter(path+"/"+ name + ".json", true);
        try {
            jsonWriter.write("[\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void closeJsonWriter() throws IOException {
        jsonWriter.write("\n]");
        jsonWriter.close();
    }

    // Create JSON entry if success
    public void writeEntry(String[] keys, String [] values) throws IOException {
        if (keys.length!=values.length){
            System.out.println("RecordLog: Keys and values do not match!");
            return;
        }

        int N = keys.length;
        String thisJson = "{";
        int c1 = 0;
        for (c1 = 0; c1 < N-1; c1++){
            thisJson += "\""+keys[c1]+"\":\""+values[c1]+"\",";
        }
        thisJson += "\""+keys[c1]+"\":\""+values[c1]+"\"},\n";

        try {
            jsonWriter.write(thisJson);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void audioRecordingEntry(String session, String timestamp, String fileName, double [] measures){
        String [] keys = new String[] {"sessionId","time","filename","th1","th1_value","th2", "th2_value",
                "th3", "th3_value"};
        String [] values = new String[] {session, timestamp, fileName, String.valueOf(measures[0]),
                String.valueOf(measures[1]), String.valueOf(measures[2]), String.valueOf(measures[3]),
                String.valueOf(measures[4]), String.valueOf(measures[5])};
        try {
            writeEntry(keys, values);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
