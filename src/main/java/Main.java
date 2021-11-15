//import swt libraries
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;

import javax.sound.sampled.*;
import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;


//specify main class
//Main.java
//Language: java
//Path: src/main/java/Main.java
public class Main {
    private static AudioFormat formatSelect;
    public static Boolean isRecording = false;
    public static Button startButton;
    public static Button stopButton;
    public static Label statusLabel;
    public static Display display;
    public static Combo combo;
    public static Shell shell;
    public static Image logoImage ;
    public static Image recordingImage ;
    public static Image warningImage ;

    //generate a SWT window
    public Main(String[] args){
        main(args);
    }
    public static void main(String[] args) {
        if (args.length > 0) {
            if (args[0].equals("server")) {
                Server server = new Server();
                server.main(args);
            }
            System.exit(0);
        }

        display = new Display();
        shell = new Shell(display);
        shell.setText("Podcastéame ésta");
        shell.setSize(700,300);
        //shell layout grid
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        shell.setLayout(gridLayout);
        //create a label
        logoImage = new Image(display, Main.class.getClassLoader().getResourceAsStream("icon.png"));
        recordingImage = new Image(display, Main.class.getClassLoader().getResourceAsStream("record.png"));
        warningImage = new Image(display, Main.class.getClassLoader().getResourceAsStream("warning.png"));
        shell.setImage(logoImage);

        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.grabExcessHorizontalSpace = false;
        gridData.widthHint = 200;
        gridData.grabExcessVerticalSpace = false;
        shell.setLayoutData(gridData);

        Settings.Load();
        //input text for host
        Label hostLabel = new Label(shell, SWT.NONE);
        hostLabel.setText("Host:");
        final Text hostText = new Text(shell, SWT.BORDER);
        hostText.setText(Settings.hostname);
        hostText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                Settings.hostname = hostText.getText();
                Settings.Save();
            }
        });
        hostText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        //input text for port
        Label portLabel = new Label(shell, SWT.NONE);
        portLabel.setText("Port:");
        final Text portText = new Text(shell, SWT.BORDER);
        portText.setText(Integer.valueOf(Settings.port).toString());
        portText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                try{
                    Settings.port = Integer.parseInt(portText.getText());
                }   catch (NumberFormatException ex){
                    Settings.port = 8080;
                    portText.setText("8080");
                }
                Settings.Save();
            }
        });
        portText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        //input text box for username
        Label label3 = new Label(shell, SWT.NONE);
        label3.setText("Enter username");
        Text text = new Text(shell, SWT.BORDER);
        text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        //on text change, update username
        text.setText(Settings.username);
        text.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                Settings.username = text.getText();
                Settings.Save();
            }
        });


        //create a droplist to select microphone device in windows
        Label label = new Label(shell, SWT.NONE);
        label.setText("Select microphone device");
        combo = new Combo(shell, SWT.READ_ONLY);
        combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        combo.setItems(getMicrophoneNames().toArray(new String[getMicrophoneNames().size()]));
        if (Settings.microphone != ""){
            combo.select(0);
            for (String s : combo.getItems()){
                if (s.equals(Settings.microphone)){
                    combo.select(combo.indexOf(s));
                }
            }
        }
        else
            combo.select(0);
        //on selection, update microphone format
        combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Settings.microphone = combo.getText();
                Settings.Save();
            }
       });


        //create a combo to select audio format
        Label label2 = new Label(shell, SWT.NONE);
        label2.setText("Select audio format");
        final Combo combo2 = new Combo(shell, SWT.READ_ONLY);
        combo2.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        ArrayList<AudioFormat> formats = getSupportedFormats(DataLine.class);
        //sort the formats
        ArrayList<String> newList = new ArrayList<>();
        for (AudioFormat format : formats) {
            if (!newList.contains(format.toString()))
                newList.add(format.toString());
        }
        newList.sort((String a, String b) -> {
            if (a.split(" ")[1] == b.split(" ")[1]) {
                return 0;
            }
            return Float.valueOf(a.split(" ")[1]) > Float.valueOf(b.split(" ")[1]) ? 1 : -1;
        }
        );

        Collections.reverse(newList);
        for (String format : newList) {
            System.out.println(format);
            combo2.add(format);
        }
        combo2.select(0);
        //on selection of audio format, update the sample rate
        combo2.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                formatSelect = getAudioFormat(combo2.getText());
            }
        }
        );
        formatSelect = getAudioFormat(combo2.getText());

        //stop recording button
        stopButton = new Button(shell, SWT.PUSH);
        stopButton.setText("Stop recording");
        stopButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                //stop recording
                stopRecording();
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
            }
         });
        stopButton.setEnabled(false);

        //create a button to start recording
        startButton = new Button(shell, SWT.PUSH);
        startButton.setText("Start recording");
        startButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        startButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                //get selected microphone device
                Mixer.Info info = getMixerInfo(combo.getText());
                //start recording
                startRecording(info);
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
            }
        });

        //create label which displays the status of the recording
        Label label4 = new Label(shell, SWT.NONE);
        label4.setText("Status: ");
        statusLabel = new Label(shell, SWT.NONE);
        statusLabel.setText("Not recording");
        statusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));


        shell.open();

        while(!shell.isDisposed()) {
            if(!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
    }


    private static void stopRecording() {
        //stop recording
        isRecording = false;
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


    private static void startRecording(Mixer.Info info) {
        try {
            isRecording = true;
            //create a new audio format
            //AudioFormat format = new AudioFormat(44100.0f, 24, 1, true, true);
            //create a new DataLine.Info
            DataLine.Info info2 = new DataLine.Info(TargetDataLine.class, formatSelect);
            //get the mixer
            Mixer mixer = AudioSystem.getMixer(info);
            //open the mixer
            TargetDataLine line = (TargetDataLine) mixer.getLine(info2);

            //create a new thread to record
            new Thread(new Runnable() {

                @Override
                public void run() {

                    try {
                        //select microphone
                        if (!line.isOpen())
                            line.open(formatSelect);

                        //start recording
                        if (!line.isRunning())
                            line.start();

                        //create a new file to write
                        String time = new java.text.SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new java.util.Date());
                        File file = new File(time+"-local-recording-"+Settings.username+".wav");
                        //create a new audio file stream
                        AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;

                        int numBytesRead;
                        byte[] data = new byte[line.getBufferSize()];

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();

                        //create a new socket
                        Socket socket = new Socket(Settings.hostname, Settings.port);
                        //create a new output stream
                        OutputStream outputStream = socket.getOutputStream();


                        /*Thread t1 = new Thread(new Runnable() {
                            public void run() {
                                //create a new input stream
                                InputStream inputStream = null;
                                try {
                                    inputStream = socket.getInputStream();
                                    byte[] inputData = new byte[1024];
                                    int numInputBytesRead;
                                    File outf = new File("local-callback"+username+".wav");
                                    if (outf.exists())
                                        outf.delete();

                                    while (true) {
                                        numInputBytesRead = inputStream.read(data);
                                        if (numInputBytesRead == -1) {
                                            break;
                                        }
                                        inputStream.read(inputData);
                                        //play inputstream
                                        FileOutputStream out = new FileOutputStream("local-callback"+username+".wav", true);
                                        out.write(data, 0, numInputBytesRead);
                                        out.close();
                                    }

                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        });
                        t1.start();*/

                        /*byte[] header = new byte[44];

                        header[0] = 'R';  // RIFF/WAVE header
                        header[1] = 'I';
                        header[2] = 'F';
                        header[3] = 'F';
                        header[4] = (byte) (0 & 0xff);
                        header[5] = (byte) ((0 >> 8) & 0xff);
                        header[6] = (byte) ((0 >> 16) & 0xff);
                        header[7] = (byte) ((0 >> 24) & 0xff);
                        header[8] = 'W';
                        header[9] = 'A';
                        header[10] = 'V';
                        header[11] = 'E';
                        header[12] = 'f';  // 'fmt ' chunk
                        header[13] = 'm';
                        header[14] = 't';
                        header[15] = ' ';
                        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
                        header[17] = 0;
                        header[18] = 0;
                        header[19] = 0;
                        header[20] = 1;  // format = 1
                        header[21] = 0;
                        header[22] = (byte) formatSelect.getChannels();
                        header[23] = 0;
                        header[24] = (byte) ((long)(1000*formatSelect.getSampleRate()) & 0xff);
                        header[25] = (byte) (((long)(1000*formatSelect.getSampleRate()) >> 8) & 0xff);
                        header[26] = (byte) (((long)(1000*formatSelect.getSampleRate()) >> 16) & 0xff);
                        header[27] = (byte) (((long)(1000*formatSelect.getSampleRate()) >> 24) & 0xff);
                        header[28] = (byte) ((long)(formatSelect.getFrameSize() * formatSelect.getFrameRate() * formatSelect.getChannels()) / 8 & 0xff);
                        header[29] = (byte) (((long)(formatSelect.getFrameSize() * formatSelect.getFrameRate() * formatSelect.getChannels()) / 8 >> 8) & 0xff);
                        header[30] = (byte) (((long)(formatSelect.getFrameSize() * formatSelect.getFrameRate() * formatSelect.getChannels()) / 8 >> 16) & 0xff);
                        header[31] = (byte) (((long)(formatSelect.getFrameSize() * formatSelect.getFrameRate() * formatSelect.getChannels()) / 8 >> 24) & 0xff);
                        header[32] = (byte) (2 * 16 / 8);  // block align
                        header[33] = 0;
                        header[34] = (byte) ((formatSelect.getSampleSizeInBits() * formatSelect.getChannels()) / 8);;  // bits per sample
                        header[35] = 0;
                        header[36] = 'd';
                        header[37] = 'a';
                        header[38] = 't';
                        header[39] = 'a';
                        header[40] = (byte) (0 & 0xff);
                        header[41] = (byte) ((0 >> 8) & 0xff);
                        header[42] = (byte) ((0 >> 16) & 0xff);
                        header[43] = (byte) ((0 >> 24) & 0xff);

                        outputStream.write(header, 0, 44);*/
                        String separator = "!!!";
                        String firstPacket = Settings.username + separator + formatSelect.toString();
                        outputStream.write(firstPacket.getBytes(StandardCharsets.UTF_8));
                        display.asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                statusLabel.setText("Recording " + file.getName());
                                startButton.setEnabled(false);
                                stopButton.setEnabled(true);
                                shell.setImage(recordingImage);
                            }
                        });

                        while (isRecording) {
                            numBytesRead = line.read(data, 0, data.length);
                            if (numBytesRead == -1) {
                                System.out.println("End of stream");
                                line.close();
                                socket.close();
                                break;
                            }
                            baos.write(data, 0, numBytesRead);
                            outputStream.write(data);

                        }

                        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(baos.toByteArray());
                        AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, formatSelect, baos.toByteArray().length / formatSelect.getFrameSize());
                        //write to file
                        AudioSystem.write(audioInputStream, fileType, file);
                        System.out.println("Recording finished");
                        //close the line
                        line.close();
                        //close the audio input stream
                        audioInputStream.close();
                        display.asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                statusLabel.setText("Recording saved: " + file.getAbsolutePath());
                                startButton.setEnabled(true);
                                stopButton.setEnabled(false);
                                shell.setImage(logoImage);
                            }
                        });
                    } catch(ConnectException ce) {
                        System.out.println("Connection refused. You need to initiate a server first.");
                        display.asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                //close the line
                                line.close();
                                statusLabel.setText("Connection refused.");
                                startButton.setEnabled(true);
                                stopButton.setEnabled(false);
                                shell.setImage(warningImage);
                            }
                        });
                        ce.printStackTrace();
                    }catch (SocketException se){
                        if (se.getMessage().equals("Connection reset by peer")) {
                            display.asyncExec(new Runnable() {
                                @Override
                                public void run() {
                                    line.close();
                                    //sleep for a second to allow the socket to close gracefully
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    Mixer.Info info = getMixerInfo(combo.getText());
                                    //start recording
                                    startRecording(info);
                                }
                            });
                        }
                        display.asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                //close the line

                                statusLabel.setText(se.getMessage());
                                startButton.setEnabled(true);
                                stopButton.setEnabled(false);
                                shell.setImage(warningImage);
                            }
                        });
                        se.printStackTrace();
                    } catch(Exception e) {
                        display.asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                //close the line
                                line.close();
                                statusLabel.setText(e.getMessage());
                                startButton.setEnabled(true);
                                stopButton.setEnabled(false);
                                shell.setImage(warningImage);
                            }
                        });
                        e.printStackTrace();
                    }
                }

            }).start();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }




    /*private static void sendData(byte[] data) {
        try {
            //open a new socket
            Socket socket = new Socket("localhost", 8080);
            //create a new data output stream
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            //write the data to the server
            out.write(data);
            out.flush();
            //close the socket
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/


    /*private static void sendData(byte[] data) {
        System.out.println("Sending data");
        try {
            HttpClient client = new HttpClient();
            GetMethod method = new GetMethod("http://localhost:8080/");
            //method.setRequestHeader("Content-Type", "audio/x-raw, layout=(string)interleaved, rate=(int)44100, format=(string)S24LE, channels=(int)1");
            System.out.println("1");
            client.executeMethod(method);
            System.out.println("2");
            method.releaseConnection();
            System.out.println("3");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("4");
    }*/



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

    private static Mixer.Info getMixerInfo(String text) {
        Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
        for (int i = 0; i < mixerInfo.length; i++) {
            if (mixerInfo[i].getName().equals(text)) {
                return mixerInfo[i];
            }
        }
        return null;
    }

    private static ArrayList<String> getMicrophoneNames() {
        Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
        System.out.println(mixerInfo.length);
        ArrayList<String> mixerNames = new ArrayList<String>();
        for (int i = 0; i < mixerInfo.length; i++) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo[i]);
            Line.Info[] targetLineInfo = mixer.getTargetLineInfo();
            for (int j = 0; j < targetLineInfo.length; j++) {
                if (targetLineInfo[j].getLineClass().equals(TargetDataLine.class)) {
                    System.out.println(mixerInfo[i].getName());
                    mixerNames.add(mixerInfo[i].getName());
                }
            }
        }
        return mixerNames;
    }



}