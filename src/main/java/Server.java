import javax.sound.sampled.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Server {

    public static ArrayList<Client> clients = new ArrayList<>();
    //server to receive audio data
    public static void main(String[] args)  {
        ServerSocket server = null;
        try {
            server = new ServerSocket(8080);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            while (true) {
                Socket socket = server.accept();
                socket.setSoTimeout(15000);
                int session = socket.getPort();

                Thread t1 = new Thread(new Runnable() {
                    public void run() {
                        try {
                            InputStream in = socket.getInputStream();
                            byte[] data = new byte[1024];
                            int numBytesRead;

                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
                            AudioFormat formatSelect = null;
                            boolean firstLine = true;

                            String username = "unknown";
                            String audioFormat = "";
                            Client thisClient = null;
                            for (Client client : clients)
                                if (client.socket == socket)
                                    thisClient = client;

                            String time = new java.text.SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new java.util.Date());
                            while (thisClient!= null && !socket.isClosed()) {
                                numBytesRead = in.read(data);
                                if (numBytesRead == -1) {
                                    break;
                                }
                                System.out.println("Received " + numBytesRead + " bytes from " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + " - " + username);


                                // RECEPCION DE AUDIO
                                if (firstLine)
                                {
                                    firstLine =false;
                                    String s = new String(data, StandardCharsets.UTF_8);
                                    System.out.println("DATA " + s);
                                    try{
                                        String[] PlainFormat = s.split("endian");
                                        String[] info = PlainFormat[0].split("!!!");
                                        audioFormat = info[1]+"endian";
                                        username = info[0];
                                        File outf = new File(time+"-"+"remote-"+username+".wav");
                                        if (outf.exists())
                                            outf.delete();
                                        System.out.println("USERNAME " + username);
                                        System.out.println("HEADERS " + audioFormat);
                                        formatSelect = getAudioFormat(audioFormat);

                                        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(baos.toByteArray());
                                        AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, formatSelect, baos.toByteArray().length / formatSelect.getFrameSize());
                                        //write to file
                                        AudioSystem.write(audioInputStream, fileType, outf);
                                    }catch (ArrayIndexOutOfBoundsException e){
                                        System.out.println("Wrong protocol");
                                        CloseSocket(socket);
                                    }
                                }
                                else
                                {
                                    //current time in human readable format in a string

                                    FileOutputStream out = new FileOutputStream(time+"-"+"remote-"+username+".wav", true);
                                    out.write(data, 0, numBytesRead);
                                    out.close();
                                    //System.out.println(new String(data));
                                }


                            }

                            System.out.println("Recording finished");
                        } catch (java.net.SocketTimeoutException se)
                        {
                            CloseSocket(socket);
                            //se.printStackTrace();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                t1.setName("Hilo"+session);
                clients.add(new Client(socket, t1));
                t1.start();
            }
        }  catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void CloseSocket(Socket socket){
        System.out.println("Socket timed out");
        ArrayList<Client> toRemove = new ArrayList<>();
        for (Client client : clients)
            if (client.socket == socket){
                toRemove.add(client);
                if (!client.socket.isClosed()) {
                    try {
                        System.out.println("Cerrando cliente: " + client.socket.getPort());
                        client.socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        clients.removeAll(toRemove);
        System.out.println("Lista clientes: ");
        for(Client s: clients){
            System.out.println(s.socket.getPort() + " - " + s.socket.isConnected());
        }
    }
    private static AudioFormat getAudioFormat(String text) {
        ArrayList<AudioFormat> formats = getSupportedFormats(DataLine.class);
        for (AudioFormat format : formats) {
            if (format.toString().equals(text)) {
                return format;
            }
        }
        return null;
    }

    public static ArrayList<AudioFormat> getSupportedFormats(Class<?> dataLineClass) {
        /*
         * These define our criteria when searching for formats supported
         * by Mixers on the system.
         */
        float sampleRates[] = { (float) 8000.0, (float) 16000.0, (float) 44100.0 };
        int channels[] = { 1, 2 };
        int bytesPerSample[] = { 2 };

        AudioFormat format;
        DataLine.Info lineInfo;

        //SystemAudioProfile profile = new SystemAudioProfile(); // Used for allocating MixerDetails below.
        ArrayList<AudioFormat> formats = new ArrayList<AudioFormat>();

        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            for (int a = 0; a < sampleRates.length; a++) {
                for (int b = 0; b < channels.length; b++) {
                    for (int c = 0; c < bytesPerSample.length; c++) {
                        format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                sampleRates[a], 8 * bytesPerSample[c], channels[b], bytesPerSample[c],
                                sampleRates[a], false);
                        lineInfo = new DataLine.Info(dataLineClass, format);
                        if (AudioSystem.isLineSupported(lineInfo)) {
                            /*
                             * TODO: To perform an exhaustive search on supported lines, we should open
                             * TODO: each Mixer and get the supported lines. Do this if this approach
                             * TODO: doesn't give decent results. For the moment, we just work with whatever
                             * TODO: the unopened mixers tell us.
                             */
                            if (AudioSystem.getMixer(mixerInfo).isLineSupported(lineInfo)) {
                                formats.add(format);
                            }
                        }
                    }
                }
            }
        }
        return formats;
    }
}
